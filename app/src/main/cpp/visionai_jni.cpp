#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <chrono>

#include "llama.h"
#include "ggml.h"
#include "ggml-backend.h"
#include "mtmd.h"
#include "mtmd-helper.h"

using steady_clock = std::chrono::steady_clock;

#define TAG "VisionAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static constexpr int MAX_TOKENS = 400;

struct VisionAIContext {
    llama_model   * model    = nullptr;
    llama_context * ctx      = nullptr;
    llama_sampler * sampler  = nullptr;
    mtmd_context  * ctx_mtmd = nullptr;
    int n_threads = 4;
};

static void throw_java_exception(JNIEnv * env, const char * msg) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls) {
        env->ThrowNew(cls, msg);
    }
}

static void create_sampler(VisionAIContext * vctx) {
    if (vctx->sampler) {
        llama_sampler_free(vctx->sampler);
    }
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    vctx->sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(vctx->sampler, llama_sampler_init_penalties(64, 1.3f, 0.0f, 0.0f));
    llama_sampler_chain_add(vctx->sampler, llama_sampler_init_min_p(0.05f, 1));
    llama_sampler_chain_add(vctx->sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(vctx->sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
}

// Apply the model's chat template to format the prompt correctly
static std::string apply_chat_template(const llama_model * model, const std::string & user_content) {
    const char * tmpl = llama_model_chat_template(model, nullptr);

    llama_chat_message messages[] = {
        { "system",    "You are an image understanding model capable of describing the salient features of any image." },
        { "user",      user_content.c_str() },
    };

    // First call: get required buffer size
    int32_t len = llama_chat_apply_template(tmpl, messages, 2, true, nullptr, 0);
    if (len < 0) {
        LOGE("chat template failed, falling back to raw prompt");
        return user_content;
    }

    std::vector<char> buf(len + 1);
    llama_chat_apply_template(tmpl, messages, 2, true, buf.data(), buf.size());
    buf[len] = '\0';

    std::string result(buf.data(), len);
    LOGI("Formatted prompt (%d chars): %.200s...", len, result.c_str());
    return result;
}

// Helper: run token generation loop, returns response string
static std::string generate_response(VisionAIContext * vctx, int max_tokens) {
    const llama_vocab * vocab = llama_model_get_vocab(vctx->model);
    std::string response;
    int tokens_generated = 0;

    auto t_gen_start = steady_clock::now();

    for (int i = 0; i < max_tokens; i++) {
        llama_token token_id = llama_sampler_sample(vctx->sampler, vctx->ctx, -1);

        if (llama_vocab_is_eog(vocab, token_id)) {
            break;
        }

        tokens_generated++;

        char buf[256];
        int n = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response.append(buf, n);
        }

        llama_batch batch = llama_batch_get_one(&token_id, 1);
        if (llama_decode(vctx->ctx, batch) != 0) {
            LOGE("Failed to decode token at position %d", i);
            break;
        }
    }

    auto t_gen_end = steady_clock::now();
    long long gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_gen_end - t_gen_start).count();
    float tok_s = tokens_generated > 0 && gen_ms > 0 ? (tokens_generated * 1000.0f / gen_ms) : 0;
    LOGI("  Generation: %lld ms (%d tokens, %.1f tok/s)", gen_ms, tokens_generated, tok_s);

    return response;
}

// Helper: streaming token generation with JNI callback
static std::string generate_response_streaming(
        VisionAIContext * vctx, int max_tokens,
        JNIEnv * env, jobject callback) {

    const llama_vocab * vocab = llama_model_get_vocab(vctx->model);
    std::string response;
    int tokens_generated = 0;

    // Cache JNI method IDs for the callback interface
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");

    auto t_gen_start = steady_clock::now();

    for (int i = 0; i < max_tokens; i++) {
        llama_token token_id = llama_sampler_sample(vctx->sampler, vctx->ctx, -1);

        if (llama_vocab_is_eog(vocab, token_id)) {
            break;
        }

        tokens_generated++;

        char buf[256];
        int n = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response.append(buf, n);

            // Call Java callback with the token piece
            jstring jtoken = env->NewStringUTF(std::string(buf, n).c_str());
            env->CallVoidMethod(callback, onTokenMethod, jtoken);
            env->DeleteLocalRef(jtoken);
        }

        llama_batch batch = llama_batch_get_one(&token_id, 1);
        if (llama_decode(vctx->ctx, batch) != 0) {
            LOGE("Failed to decode token at position %d", i);
            break;
        }
    }

    auto t_gen_end = steady_clock::now();
    long long gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_gen_end - t_gen_start).count();
    float tok_s = tokens_generated > 0 && gen_ms > 0 ? (tokens_generated * 1000.0f / gen_ms) : 0;
    LOGI("  Generation (streaming): %lld ms (%d tokens, %.1f tok/s)", gen_ms, tokens_generated, tok_s);

    return response;
}

extern "C" {

// Load the LLM model + multimodal projector
JNIEXPORT jlong JNICALL
Java_com_example_visionai_inference_LlamaModel_loadModel(
        JNIEnv * env, jobject /* thiz */,
        jstring model_path, jstring mmproj_path,
        jint n_threads, jint n_ctx) {

    const char * model_path_c  = env->GetStringUTFChars(model_path, nullptr);
    const char * mmproj_path_c = env->GetStringUTFChars(mmproj_path, nullptr);

    auto * vctx = new VisionAIContext();
    vctx->n_threads = n_threads;

    LOGI("Loading model: %s", model_path_c);
    LOGI("Loading mmproj: %s", mmproj_path_c);

    ggml_backend_load_all();

    // Log available backends
    size_t n_backends = ggml_backend_dev_count();
    LOGI("Available backends: %zu", n_backends);
    for (size_t i = 0; i < n_backends; i++) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        LOGI("  Backend %zu: %s (%s)", i, ggml_backend_dev_name(dev), ggml_backend_dev_description(dev));
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 99; // Offload all layers to GPU (OpenCL/Adreno)
    vctx->model = llama_model_load_from_file(model_path_c, model_params);

    if (!vctx->model) {
        LOGE("Failed to load model");
        env->ReleaseStringUTFChars(model_path, model_path_c);
        env->ReleaseStringUTFChars(mmproj_path, mmproj_path_c);
        delete vctx;
        throw_java_exception(env, "Failed to load model");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx            = n_ctx;
    ctx_params.n_batch          = 512;  // Larger batches for faster prompt eval
    ctx_params.n_threads        = n_threads;
    ctx_params.flash_attn_type  = LLAMA_FLASH_ATTN_TYPE_ENABLED;
    vctx->ctx = llama_init_from_model(vctx->model, ctx_params);

    if (!vctx->ctx) {
        LOGE("Failed to create llama context");
        llama_model_free(vctx->model);
        env->ReleaseStringUTFChars(model_path, model_path_c);
        env->ReleaseStringUTFChars(mmproj_path, mmproj_path_c);
        delete vctx;
        throw_java_exception(env, "Failed to create llama context");
        return 0;
    }

    mtmd_context_params mparams = mtmd_context_params_default();
    mparams.use_gpu   = true; // Use GPU (OpenCL/Adreno) for vision encoder too
    mparams.n_threads = n_threads;

    vctx->ctx_mtmd = mtmd_init_from_file(mmproj_path_c, vctx->model, mparams);

    if (!vctx->ctx_mtmd) {
        LOGE("Failed to load multimodal projector");
        llama_free(vctx->ctx);
        llama_model_free(vctx->model);
        env->ReleaseStringUTFChars(model_path, model_path_c);
        env->ReleaseStringUTFChars(mmproj_path, mmproj_path_c);
        delete vctx;
        throw_java_exception(env, "Failed to load multimodal projector");
        return 0;
    }

    create_sampler(vctx);

    env->ReleaseStringUTFChars(model_path, model_path_c);
    env->ReleaseStringUTFChars(mmproj_path, mmproj_path_c);

    const char * chat_tmpl = llama_model_chat_template(vctx->model, nullptr);
    LOGI("Model loaded successfully, vision support: %s, chat template: %s",
         mtmd_support_vision(vctx->ctx_mtmd) ? "yes" : "no",
         chat_tmpl ? "yes" : "no");

    return reinterpret_cast<jlong>(vctx);
}

// Single image inference
JNIEXPORT jstring JNICALL
Java_com_example_visionai_inference_LlamaModel_runInference(
        JNIEnv * env, jobject /* thiz */,
        jlong ctx_ptr, jbyteArray image_bytes, jint width, jint height,
        jstring prompt) {

    auto * vctx = reinterpret_cast<VisionAIContext *>(ctx_ptr);
    if (!vctx || !vctx->model || !vctx->ctx || !vctx->ctx_mtmd) {
        throw_java_exception(env, "Model not loaded");
        return env->NewStringUTF("");
    }

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    jbyte * img_data = env->GetByteArrayElements(image_bytes, nullptr);

    LOGI("Running inference: %dx%d image", width, height);
    auto t_start = steady_clock::now();

    mtmd_bitmap * bmp = mtmd_bitmap_init(
        (uint32_t)width, (uint32_t)height,
        reinterpret_cast<const unsigned char *>(img_data)
    );

    // Build user content with image marker + prompt, then apply chat template
    const char * marker = mtmd_default_marker();
    std::string user_content = std::string(marker) + "\n" + prompt_c;
    std::string formatted = apply_chat_template(vctx->model, user_content);

    mtmd_input_text text;
    text.text          = formatted.c_str();
    text.add_special   = false; // chat template already includes BOS
    text.parse_special = true;

    const mtmd_bitmap * bitmaps[] = { bmp };
    mtmd_input_chunks * chunks = mtmd_input_chunks_init();

    int32_t tokenize_res = mtmd_tokenize(vctx->ctx_mtmd, chunks, &text, bitmaps, 1);
    if (tokenize_res != 0) {
        LOGE("Failed to tokenize, error: %d", tokenize_res);
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bmp);
        env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        throw_java_exception(env, "Failed to tokenize input");
        return env->NewStringUTF("");
    }

    llama_memory_clear(llama_get_memory(vctx->ctx), true);
    create_sampler(vctx); // Reset sampler state

    auto t_after_tokenize = steady_clock::now();

    llama_pos n_past = 0;
    int32_t eval_res = mtmd_helper_eval_chunks(
        vctx->ctx_mtmd, vctx->ctx, chunks,
        n_past, 0, 128, true, &n_past
    );

    if (eval_res != 0) {
        LOGE("Failed to evaluate chunks, error: %d", eval_res);
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bmp);
        env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        throw_java_exception(env, "Failed to evaluate input");
        return env->NewStringUTF("");
    }

    auto t_after_eval = steady_clock::now();

    std::string response = generate_response(vctx, MAX_TOKENS);

    auto t_end = steady_clock::now();
    auto ms = [](steady_clock::time_point a, steady_clock::time_point b) {
        return std::chrono::duration_cast<std::chrono::milliseconds>(b - a).count();
    };
    LOGI("=== PHOTO BENCHMARK === Tokenize: %lld ms | Eval: %lld ms | Total: %lld ms",
         ms(t_start, t_after_tokenize), ms(t_after_tokenize, t_after_eval), ms(t_start, t_end));

    mtmd_input_chunks_free(chunks);
    mtmd_bitmap_free(bmp);
    env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    return env->NewStringUTF(response.c_str());
}

// Multi-frame video inference
JNIEXPORT jstring JNICALL
Java_com_example_visionai_inference_LlamaModel_runVideoInference(
        JNIEnv * env, jobject /* thiz */,
        jlong ctx_ptr, jobjectArray frames_array, jintArray widths, jintArray heights,
        jstring prompt) {

    auto * vctx = reinterpret_cast<VisionAIContext *>(ctx_ptr);
    if (!vctx || !vctx->model || !vctx->ctx || !vctx->ctx_mtmd) {
        throw_java_exception(env, "Model not loaded");
        return env->NewStringUTF("");
    }

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    int n_frames = env->GetArrayLength(frames_array);
    jint * w_arr = env->GetIntArrayElements(widths, nullptr);
    jint * h_arr = env->GetIntArrayElements(heights, nullptr);

    LOGI("Running video inference: %d frames", n_frames);
    auto t_start = steady_clock::now();

    // Create bitmaps for each frame
    std::vector<mtmd_bitmap *> bitmaps(n_frames);
    std::vector<jbyte *> frame_ptrs(n_frames);
    std::vector<jbyteArray> frame_refs(n_frames);

    for (int i = 0; i < n_frames; i++) {
        frame_refs[i] = (jbyteArray)env->GetObjectArrayElement(frames_array, i);
        frame_ptrs[i] = env->GetByteArrayElements(frame_refs[i], nullptr);
        bitmaps[i] = mtmd_bitmap_init(
            (uint32_t)w_arr[i], (uint32_t)h_arr[i],
            reinterpret_cast<const unsigned char *>(frame_ptrs[i])
        );
        LOGI("  Frame %d: %dx%d", i, w_arr[i], h_arr[i]);
    }

    // Build user content with prompt BEFORE markers so the instruction has more weight
    const char * marker = mtmd_default_marker();
    std::string user_content = std::string(prompt_c) + "\n";
    for (int i = 0; i < n_frames; i++) {
        user_content += std::string(marker) + "\n";
    }
    std::string formatted = apply_chat_template(vctx->model, user_content);

    mtmd_input_text text;
    text.text          = formatted.c_str();
    text.add_special   = false; // chat template already includes BOS
    text.parse_special = true;

    // Convert to const pointer array
    std::vector<const mtmd_bitmap *> bitmap_ptrs(bitmaps.begin(), bitmaps.end());
    mtmd_input_chunks * chunks = mtmd_input_chunks_init();

    int32_t tokenize_res = mtmd_tokenize(vctx->ctx_mtmd, chunks, &text, bitmap_ptrs.data(), n_frames);
    if (tokenize_res != 0) {
        LOGE("Failed to tokenize video, error: %d", tokenize_res);
        for (int i = 0; i < n_frames; i++) {
            mtmd_bitmap_free(bitmaps[i]);
            env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
        }
        mtmd_input_chunks_free(chunks);
        env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
        env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        throw_java_exception(env, "Failed to tokenize video input");
        return env->NewStringUTF("");
    }

    llama_memory_clear(llama_get_memory(vctx->ctx), true);
    create_sampler(vctx);

    auto t_after_tokenize = steady_clock::now();

    llama_pos n_past = 0;
    int32_t eval_res = mtmd_helper_eval_chunks(
        vctx->ctx_mtmd, vctx->ctx, chunks,
        n_past, 0, 128, true, &n_past
    );

    if (eval_res != 0) {
        LOGE("Failed to evaluate video chunks, error: %d", eval_res);
        for (int i = 0; i < n_frames; i++) {
            mtmd_bitmap_free(bitmaps[i]);
            env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
        }
        mtmd_input_chunks_free(chunks);
        env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
        env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        throw_java_exception(env, "Failed to evaluate video input");
        return env->NewStringUTF("");
    }

    auto t_after_eval = steady_clock::now();

    std::string response = generate_response(vctx, MAX_TOKENS);

    auto t_end = steady_clock::now();
    auto ms = [](steady_clock::time_point a, steady_clock::time_point b) {
        return std::chrono::duration_cast<std::chrono::milliseconds>(b - a).count();
    };
    LOGI("=== VIDEO BENCHMARK === Frames: %d | Tokenize: %lld ms | Eval: %lld ms | Total: %lld ms",
         n_frames, ms(t_start, t_after_tokenize), ms(t_after_tokenize, t_after_eval), ms(t_start, t_end));

    // Cleanup
    for (int i = 0; i < n_frames; i++) {
        mtmd_bitmap_free(bitmaps[i]);
        env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
    }
    mtmd_input_chunks_free(chunks);
    env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
    env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    return env->NewStringUTF(response.c_str());
}

// Single image inference — streaming version
JNIEXPORT void JNICALL
Java_com_example_visionai_inference_LlamaModel_runInferenceStreaming(
        JNIEnv * env, jobject /* thiz */,
        jlong ctx_ptr, jbyteArray image_bytes, jint width, jint height,
        jstring prompt, jobject callback) {

    auto * vctx = reinterpret_cast<VisionAIContext *>(ctx_ptr);
    if (!vctx || !vctx->model || !vctx->ctx || !vctx->ctx_mtmd) {
        throw_java_exception(env, "Model not loaded");
        return;
    }

    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onCompleteMethod = env->GetMethodID(cbClass, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onErrorMethod = env->GetMethodID(cbClass, "onError", "(Ljava/lang/String;)V");

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    jbyte * img_data = env->GetByteArrayElements(image_bytes, nullptr);

    LOGI("Running streaming inference: %dx%d image", width, height);
    auto t_start = steady_clock::now();

    mtmd_bitmap * bmp = mtmd_bitmap_init(
        (uint32_t)width, (uint32_t)height,
        reinterpret_cast<const unsigned char *>(img_data)
    );

    const char * marker = mtmd_default_marker();
    std::string user_content = std::string(marker) + "\n" + prompt_c;
    std::string formatted = apply_chat_template(vctx->model, user_content);

    mtmd_input_text text;
    text.text          = formatted.c_str();
    text.add_special   = false;
    text.parse_special = true;

    const mtmd_bitmap * bitmaps[] = { bmp };
    mtmd_input_chunks * chunks = mtmd_input_chunks_init();

    int32_t tokenize_res = mtmd_tokenize(vctx->ctx_mtmd, chunks, &text, bitmaps, 1);
    if (tokenize_res != 0) {
        LOGE("Failed to tokenize, error: %d", tokenize_res);
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bmp);
        env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        jstring jerr = env->NewStringUTF("Failed to tokenize input");
        env->CallVoidMethod(callback, onErrorMethod, jerr);
        env->DeleteLocalRef(jerr);
        return;
    }

    llama_memory_clear(llama_get_memory(vctx->ctx), true);
    create_sampler(vctx);

    llama_pos n_past = 0;
    int32_t eval_res = mtmd_helper_eval_chunks(
        vctx->ctx_mtmd, vctx->ctx, chunks,
        n_past, 0, 128, true, &n_past
    );

    if (eval_res != 0) {
        LOGE("Failed to evaluate chunks, error: %d", eval_res);
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bmp);
        env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        jstring jerr = env->NewStringUTF("Failed to evaluate input");
        env->CallVoidMethod(callback, onErrorMethod, jerr);
        env->DeleteLocalRef(jerr);
        return;
    }

    std::string response = generate_response_streaming(vctx, MAX_TOKENS, env, callback);

    auto t_end = steady_clock::now();
    auto ms = [](steady_clock::time_point a, steady_clock::time_point b) {
        return std::chrono::duration_cast<std::chrono::milliseconds>(b - a).count();
    };
    LOGI("=== PHOTO STREAMING BENCHMARK === Total: %lld ms", ms(t_start, t_end));

    mtmd_input_chunks_free(chunks);
    mtmd_bitmap_free(bmp);
    env->ReleaseByteArrayElements(image_bytes, img_data, JNI_ABORT);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    jstring jresult = env->NewStringUTF(response.c_str());
    env->CallVoidMethod(callback, onCompleteMethod, jresult);
    env->DeleteLocalRef(jresult);
}

// Multi-frame video inference — streaming version
JNIEXPORT void JNICALL
Java_com_example_visionai_inference_LlamaModel_runVideoInferenceStreaming(
        JNIEnv * env, jobject /* thiz */,
        jlong ctx_ptr, jobjectArray frames_array, jintArray widths, jintArray heights,
        jstring prompt, jobject callback) {

    auto * vctx = reinterpret_cast<VisionAIContext *>(ctx_ptr);
    if (!vctx || !vctx->model || !vctx->ctx || !vctx->ctx_mtmd) {
        throw_java_exception(env, "Model not loaded");
        return;
    }

    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onCompleteMethod = env->GetMethodID(cbClass, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onErrorMethod = env->GetMethodID(cbClass, "onError", "(Ljava/lang/String;)V");

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    int n_frames = env->GetArrayLength(frames_array);
    jint * w_arr = env->GetIntArrayElements(widths, nullptr);
    jint * h_arr = env->GetIntArrayElements(heights, nullptr);

    LOGI("Running streaming video inference: %d frames", n_frames);
    auto t_start = steady_clock::now();

    std::vector<mtmd_bitmap *> bitmaps(n_frames);
    std::vector<jbyte *> frame_ptrs(n_frames);
    std::vector<jbyteArray> frame_refs(n_frames);

    for (int i = 0; i < n_frames; i++) {
        frame_refs[i] = (jbyteArray)env->GetObjectArrayElement(frames_array, i);
        frame_ptrs[i] = env->GetByteArrayElements(frame_refs[i], nullptr);
        bitmaps[i] = mtmd_bitmap_init(
            (uint32_t)w_arr[i], (uint32_t)h_arr[i],
            reinterpret_cast<const unsigned char *>(frame_ptrs[i])
        );
    }

    const char * marker = mtmd_default_marker();
    std::string user_content = std::string(prompt_c) + "\n";
    for (int i = 0; i < n_frames; i++) {
        user_content += std::string(marker) + "\n";
    }
    std::string formatted = apply_chat_template(vctx->model, user_content);

    mtmd_input_text text;
    text.text          = formatted.c_str();
    text.add_special   = false;
    text.parse_special = true;

    std::vector<const mtmd_bitmap *> bitmap_ptrs(bitmaps.begin(), bitmaps.end());
    mtmd_input_chunks * chunks = mtmd_input_chunks_init();

    int32_t tokenize_res = mtmd_tokenize(vctx->ctx_mtmd, chunks, &text, bitmap_ptrs.data(), n_frames);
    if (tokenize_res != 0) {
        LOGE("Failed to tokenize video, error: %d", tokenize_res);
        for (int i = 0; i < n_frames; i++) {
            mtmd_bitmap_free(bitmaps[i]);
            env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
        }
        mtmd_input_chunks_free(chunks);
        env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
        env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        jstring jerr = env->NewStringUTF("Failed to tokenize video input");
        env->CallVoidMethod(callback, onErrorMethod, jerr);
        env->DeleteLocalRef(jerr);
        return;
    }

    llama_memory_clear(llama_get_memory(vctx->ctx), true);
    create_sampler(vctx);

    llama_pos n_past = 0;
    int32_t eval_res = mtmd_helper_eval_chunks(
        vctx->ctx_mtmd, vctx->ctx, chunks,
        n_past, 0, 128, true, &n_past
    );

    if (eval_res != 0) {
        LOGE("Failed to evaluate video chunks, error: %d", eval_res);
        for (int i = 0; i < n_frames; i++) {
            mtmd_bitmap_free(bitmaps[i]);
            env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
        }
        mtmd_input_chunks_free(chunks);
        env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
        env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
        env->ReleaseStringUTFChars(prompt, prompt_c);
        jstring jerr = env->NewStringUTF("Failed to evaluate video input");
        env->CallVoidMethod(callback, onErrorMethod, jerr);
        env->DeleteLocalRef(jerr);
        return;
    }

    std::string response = generate_response_streaming(vctx, MAX_TOKENS, env, callback);

    auto t_end = steady_clock::now();
    auto ms = [](steady_clock::time_point a, steady_clock::time_point b) {
        return std::chrono::duration_cast<std::chrono::milliseconds>(b - a).count();
    };
    LOGI("=== VIDEO STREAMING BENCHMARK === Frames: %d | Total: %lld ms", n_frames, ms(t_start, t_end));

    for (int i = 0; i < n_frames; i++) {
        mtmd_bitmap_free(bitmaps[i]);
        env->ReleaseByteArrayElements(frame_refs[i], frame_ptrs[i], JNI_ABORT);
    }
    mtmd_input_chunks_free(chunks);
    env->ReleaseIntArrayElements(widths, w_arr, JNI_ABORT);
    env->ReleaseIntArrayElements(heights, h_arr, JNI_ABORT);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    jstring jresult = env->NewStringUTF(response.c_str());
    env->CallVoidMethod(callback, onCompleteMethod, jresult);
    env->DeleteLocalRef(jresult);
}

// Free all resources
JNIEXPORT void JNICALL
Java_com_example_visionai_inference_LlamaModel_freeModel(
        JNIEnv * env, jobject /* thiz */, jlong ctx_ptr) {

    auto * vctx = reinterpret_cast<VisionAIContext *>(ctx_ptr);
    if (!vctx) return;

    LOGI("Freeing model resources");

    if (vctx->sampler)  llama_sampler_free(vctx->sampler);
    if (vctx->ctx_mtmd) mtmd_free(vctx->ctx_mtmd);
    if (vctx->ctx)      llama_free(vctx->ctx);
    if (vctx->model)    llama_model_free(vctx->model);

    delete vctx;
}

} // extern "C"
