cmake_minimum_required(VERSION 3.22.1)
project("visionai")

find_package(Threads REQUIRED)

# Path to llama.cpp (cloned as submodule at project root)
set(LLAMA_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../llama.cpp")
get_filename_component(LLAMA_CPP_DIR ${LLAMA_CPP_DIR} ABSOLUTE)

set(MTMD_DIR "${LLAMA_CPP_DIR}/tools/mtmd")

# Don't build tools or examples from llama.cpp
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

# OpenCL for Adreno GPU: use our custom FindOpenCL that provides headers + stub
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

# Build llama.cpp core (ggml + llama)
add_subdirectory(${LLAMA_CPP_DIR} llama.cpp)

# Build mtmd library manually (without the CLI executables)
add_library(mtmd STATIC
    ${MTMD_DIR}/mtmd.cpp
    ${MTMD_DIR}/mtmd-audio.cpp
    ${MTMD_DIR}/mtmd-helper.cpp
    ${MTMD_DIR}/clip.cpp
    ${MTMD_DIR}/models/cogvlm.cpp
    ${MTMD_DIR}/models/conformer.cpp
    ${MTMD_DIR}/models/glm4v.cpp
    ${MTMD_DIR}/models/internvl.cpp
    ${MTMD_DIR}/models/kimivl.cpp
    ${MTMD_DIR}/models/kimik25.cpp
    ${MTMD_DIR}/models/nemotron-v2-vl.cpp
    ${MTMD_DIR}/models/llama4.cpp
    ${MTMD_DIR}/models/llava.cpp
    ${MTMD_DIR}/models/minicpmv.cpp
    ${MTMD_DIR}/models/paddleocr.cpp
    ${MTMD_DIR}/models/pixtral.cpp
    ${MTMD_DIR}/models/qwen2vl.cpp
    ${MTMD_DIR}/models/qwen3vl.cpp
    ${MTMD_DIR}/models/siglip.cpp
    ${MTMD_DIR}/models/whisper-enc.cpp
    ${MTMD_DIR}/models/mobilenetv5.cpp
    ${MTMD_DIR}/models/youtuvl.cpp
)

target_link_libraries(mtmd PUBLIC ggml llama)
target_link_libraries(mtmd PRIVATE Threads::Threads)
target_include_directories(mtmd PUBLIC ${MTMD_DIR})
target_include_directories(mtmd PRIVATE ${LLAMA_CPP_DIR})
target_include_directories(mtmd PRIVATE ${LLAMA_CPP_DIR}/vendor)
target_compile_features(mtmd PRIVATE cxx_std_17)
target_compile_options(mtmd PRIVATE -Wno-cast-qual)
set_target_properties(mtmd PROPERTIES POSITION_INDEPENDENT_CODE ON)

# Our JNI bridge library
add_library(visionai SHARED visionai_jni.cpp)

target_include_directories(visionai PRIVATE
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/ggml/include
    ${MTMD_DIR}
)

target_link_libraries(visionai
    android
    log
    llama
    mtmd
)

target_compile_features(visionai PRIVATE cxx_std_17)

target_compile_options(visionai PRIVATE
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -O3
)

target_link_options(visionai PRIVATE
    -Wl,--gc-sections
    -flto
)
