cmake_minimum_required(VERSION 3.10)

project("EdgeViewNative")

# --- OpenCV Configuration ---
# Set the path to your prebuilt OpenCV .so file. This path is relative
# to this CMakeLists.txt file. Update the path for your project structure.
# Using ${ANDROID_ABI} makes it work for different architectures.
set(OPENCV_LIB_PATH ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so)

# Add the shared library from the specified C++ source files
add_library(native-lib SHARED
            process.cpp
            native_utils.cpp)

# Add the include path for your own header files
include_directories(../cpp/include)

# Find the Android logging library
find_library(log-lib
             log)

# Link all required libraries to your native-lib
target_link_libraries(native-lib
                      ${OPENCV_LIB_PATH}
                      ${log-lib})
