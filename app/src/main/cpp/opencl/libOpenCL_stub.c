// Minimal OpenCL stub for linking during Android cross-compilation.
// The real libOpenCL.so is loaded at runtime from the device (/system/vendor/lib64/).
// All functions return error codes; they are never actually called.

#include <CL/cl.h>

cl_int clGetPlatformIDs(cl_uint n, cl_platform_id *p, cl_uint *num) { return -1; }
cl_int clGetPlatformInfo(cl_platform_id p, cl_platform_info i, size_t s, void *v, size_t *r) { return -1; }
cl_int clGetDeviceIDs(cl_platform_id p, cl_device_type t, cl_uint n, cl_device_id *d, cl_uint *num) { return -1; }
cl_int clGetDeviceInfo(cl_device_id d, cl_device_info i, size_t s, void *v, size_t *r) { return -1; }

cl_context clCreateContext(const cl_context_properties *p, cl_uint n, const cl_device_id *d,
    void (CL_CALLBACK *cb)(const char *, const void *, size_t, void *), void *ud, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_command_queue clCreateCommandQueue(cl_context c, cl_device_id d, cl_command_queue_properties p, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_mem clCreateBuffer(cl_context c, cl_mem_flags f, size_t s, void *hp, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_mem clCreateImage(cl_context c, cl_mem_flags f, const cl_image_format *fmt, const cl_image_desc *desc, void *hp, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_mem clCreateSubBuffer(cl_mem b, cl_mem_flags f, cl_buffer_create_type t, const void *i, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_program clCreateProgramWithSource(cl_context c, cl_uint cnt, const char **s, const size_t *l, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_int clBuildProgram(cl_program p, cl_uint n, const cl_device_id *d, const char *o,
    void (CL_CALLBACK *cb)(cl_program, void *), void *ud) { return -1; }

cl_int clGetProgramBuildInfo(cl_program p, cl_device_id d, cl_program_build_info i, size_t s, void *v, size_t *r) { return -1; }

cl_kernel clCreateKernel(cl_program p, const char *n, cl_int *e) {
    if (e) *e = -1; return 0;
}

cl_int clSetKernelArg(cl_kernel k, cl_uint i, size_t s, const void *v) { return -1; }
cl_int clGetKernelInfo(cl_kernel k, cl_kernel_info i, size_t s, void *v, size_t *r) { return -1; }
cl_int clGetKernelWorkGroupInfo(cl_kernel k, cl_device_id d, cl_kernel_work_group_info i, size_t s, void *v, size_t *r) { return -1; }
cl_int clGetKernelSubGroupInfo(cl_kernel k, cl_device_id d, cl_kernel_sub_group_info i, size_t is, const void *iv, size_t s, void *v, size_t *r) { return -1; }

cl_int clEnqueueNDRangeKernel(cl_command_queue q, cl_kernel k, cl_uint d, const size_t *go,
    const size_t *gs, const size_t *ls, cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueReadBuffer(cl_command_queue q, cl_mem b, cl_bool bw, size_t o, size_t s,
    void *p, cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueWriteBuffer(cl_command_queue q, cl_mem b, cl_bool bw, size_t o, size_t s,
    const void *p, cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueCopyBuffer(cl_command_queue q, cl_mem s, cl_mem d, size_t so, size_t do2, size_t sz,
    cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueFillBuffer(cl_command_queue q, cl_mem b, const void *p, size_t ps, size_t o, size_t s,
    cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueBarrierWithWaitList(cl_command_queue q, cl_uint n, const cl_event *wl, cl_event *e) { return -1; }
cl_int clEnqueueMarkerWithWaitList(cl_command_queue q, cl_uint n, const cl_event *wl, cl_event *e) { return -1; }

cl_int clFinish(cl_command_queue q) { return -1; }
cl_int clFlush(cl_command_queue q) { return -1; }

cl_int clReleaseMemObject(cl_mem m) { return -1; }
cl_int clReleaseProgram(cl_program p) { return -1; }
cl_int clReleaseContext(cl_context c) { return -1; }
cl_int clReleaseEvent(cl_event e) { return -1; }

cl_int clWaitForEvents(cl_uint n, const cl_event *el) { return -1; }
cl_int clGetEventProfilingInfo(cl_event e, cl_profiling_info i, size_t s, void *v, size_t *r) { return -1; }
