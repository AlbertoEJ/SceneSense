# Custom FindOpenCL for Android cross-compilation targeting Adreno GPUs.
# Provides OpenCL headers for compilation and links against a stub library.
# The real libOpenCL.so is loaded from the device at runtime.

set(OPENCL_ROOT "${CMAKE_CURRENT_LIST_DIR}/../opencl")

set(OpenCL_FOUND TRUE)
set(OpenCL_INCLUDE_DIRS "${OPENCL_ROOT}")
set(OpenCL_VERSION_STRING "3.0")

# Build a stub shared library that satisfies the linker
if(NOT TARGET OpenCL::OpenCL)
    add_library(opencl_stub SHARED "${OPENCL_ROOT}/libOpenCL_stub.c")
    target_include_directories(opencl_stub PUBLIC "${OPENCL_ROOT}")
    set_target_properties(opencl_stub PROPERTIES OUTPUT_NAME "OpenCL")

    # Create the imported target that find_package consumers expect
    add_library(OpenCL::OpenCL ALIAS opencl_stub)
    set(OpenCL_LIBRARIES opencl_stub)
    set(OpenCL_LIBRARY "$<TARGET_FILE:opencl_stub>")
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(OpenCL
    REQUIRED_VARS OpenCL_LIBRARIES OpenCL_INCLUDE_DIRS
    VERSION_VAR OpenCL_VERSION_STRING
)
