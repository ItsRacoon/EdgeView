cmake_minimum_required(VERSION 3.22.1)
project(edgeview LANGUAGES C CXX)

set(SRC_FILES
    process.cpp
    native_utils.cpp
    edgeview.cpp
)

add_library(native-lib SHARED ${SRC_FILES})

target_include_directories(native-lib PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

find_library(log-lib log)

set(OPENCV_LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI})
set(OPENCV_LIB ${OPENCV_LIB_DIR}/libopencv_java4.so)

if(EXISTS ${OPENCV_LIB})
    message(STATUS "Linking OpenCV from: ${OPENCV_LIB}")
    add_library(opencv_java4 SHARED IMPORTED)
    set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION ${OPENCV_LIB})
else()
    message(FATAL_ERROR "OpenCV .so not found for ABI ${CMAKE_ANDROID_ARCH_ABI}. Expected: ${OPENCV_LIB}\n    Place libopencv_java4.so under app/src/main/jniLibs/<abi>/libopencv_java4.so")
endif()

target_link_libraries(native-lib PRIVATE opencv_java4 ${log-lib} c++_static)

set_target_properties(native-lib PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)
