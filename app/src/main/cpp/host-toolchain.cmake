set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_MAKE_PROGRAM "C:/Users/alber/AppData/Local/Android/Sdk/cmake/3.22.1/bin/ninja.exe" CACHE FILEPATH "" FORCE)

# MSVC paths
set(MSVC_ROOT "C:/Program Files (x86)/Microsoft Visual Studio/2019/BuildTools/VC/Tools/MSVC/14.29.30133")
set(WINSDK_ROOT "C:/Program Files (x86)/Windows Kits/10")
set(WINSDK_VERSION "10.0.19041.0")

# Compilers
set(CMAKE_C_COMPILER "${MSVC_ROOT}/bin/Hostx64/x64/cl.exe")
set(CMAKE_CXX_COMPILER "${MSVC_ROOT}/bin/Hostx64/x64/cl.exe")
set(CMAKE_LINKER "${MSVC_ROOT}/bin/Hostx64/x64/link.exe")
set(CMAKE_RC_COMPILER "${WINSDK_ROOT}/bin/${WINSDK_VERSION}/x64/rc.exe")
set(CMAKE_MT "${WINSDK_ROOT}/bin/${WINSDK_VERSION}/x64/mt.exe")

# Include directories
include_directories(SYSTEM
    "${MSVC_ROOT}/include"
    "${WINSDK_ROOT}/Include/${WINSDK_VERSION}/ucrt"
    "${WINSDK_ROOT}/Include/${WINSDK_VERSION}/shared"
    "${WINSDK_ROOT}/Include/${WINSDK_VERSION}/um"
    "${WINSDK_ROOT}/Include/${WINSDK_VERSION}/winrt"
)

# Library directories
link_directories(
    "${MSVC_ROOT}/lib/x64"
    "${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/ucrt/x64"
    "${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/um/x64"
)

# Set environment-like flags for cl.exe
set(CMAKE_C_FLAGS_INIT "/I\"${MSVC_ROOT}/include\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/ucrt\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/shared\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/um\"")
set(CMAKE_CXX_FLAGS_INIT "/I\"${MSVC_ROOT}/include\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/ucrt\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/shared\" /I\"${WINSDK_ROOT}/Include/${WINSDK_VERSION}/um\"")

set(CMAKE_EXE_LINKER_FLAGS_INIT "/LIBPATH:\"${MSVC_ROOT}/lib/x64\" /LIBPATH:\"${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/ucrt/x64\" /LIBPATH:\"${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/um/x64\"")
set(CMAKE_SHARED_LINKER_FLAGS_INIT "/LIBPATH:\"${MSVC_ROOT}/lib/x64\" /LIBPATH:\"${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/ucrt/x64\" /LIBPATH:\"${WINSDK_ROOT}/Lib/${WINSDK_VERSION}/um/x64\"")
