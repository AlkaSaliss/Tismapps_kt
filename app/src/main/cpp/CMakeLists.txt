
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
set(TARGET tismapps_cpp)
project("tismapps" CXX)

# Relative path of gradle build directory to CMakeLists.txt
#set(build_DIR ${CMAKE_SOURCE_DIR}/build)
set(APP_DIR $ENV{HOME}/AndroidStudioProjects/Tismapps)
set(build_DIR ${APP_DIR}/app/build)

file(GLOB PYTORCH_INCLUDE_DIRS "${build_DIR}/pytorch_android*.aar/headers")
file(GLOB PYTORCH_LINK_DIRS "${build_DIR}/pytorch_android*.aar/jni/${ANDROID_ABI}")

set(OpenCV_STATIC on)
set(OpenCV_DIR /home/alka/opencv-4.7.0-android-sdk/OpenCV-android-sdk/sdk/native/jni)

find_library(PYTORCH_LIBRARY pytorch_jni
        PATHS ${PYTORCH_LINK_DIRS}
        NO_CMAKE_FIND_ROOT_PATH)

find_library(FBJNI_LIBRARY fbjni
        PATHS ${PYTORCH_LINK_DIRS}
        NO_CMAKE_FIND_ROOT_PATH)

include_directories(
    ${PYTORCH_INCLUDE_DIRS}
    ${OpenCV_DIR}/include
)


#/home/alka/AndroidStudioProjects/Tismapps/app/src/main/cpp/CMakeLists.txt
#/home/alka/AndroidStudioProjects/Tismapps/app/build.gradle
#/home/alka/AndroidStudioProjects/Tismapps/build

#############OPENCV CONF

#"/home/alka/libtorch-shared-with-deps-2.0.1+cpu/libtorch/share/cmake/Torch"

#list(APPEND CMAKE_FIND_ROOT_PATH "/home/alka/libtorch-shared-with-deps-2.0.1+cpu/libtorch")
#list(APPEND CMAKE_PREFIX_PATH "/home/alka/libtorch-shared-with-deps-2.0.1+cpu/libtorch/share/cmake/Torch")
#set(CMAKE_PREFIX_PATH /home/alka/libtorch-shared-with-deps-2.0.1+cpu)
#set(Torch_DIR /home/alka/libtorch-shared-with-deps-2.0.1+cpu/libtorch/share/cmake/Torch)
#set(Caffe2_DIR /home/alka/libtorch-cxx11-abi-shared-with-deps-2.0.1+cpu/libtorch/share/cmake/Caffe2)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${TORCH_CXX_FLAGS}")


set(BUILD_SUBDIR ${ANDROID_ABI})


find_package(OpenCV REQUIRED)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             tismapps_cpp

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             tismapps.cpp
            cvutils.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

find_library(jnigraphics-lib jnigraphics)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        tismapps_cpp PRIVATE

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
        ${OpenCV_LIBS}
        ${jnigraphics-lib}
        ${FBJNI_LIBRARY}
        ${PYTORCH_LIBRARY}
        )
