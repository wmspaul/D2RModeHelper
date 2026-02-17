# Require at least CMake 3.5
cmake_minimum_required(VERSION 3.5)

project(D2RCascCLI)

# Set the output directory for binaries, libraries
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")

# Check if the CascLib submodule exists and is updated
if (NOT EXISTS "${CMAKE_SOURCE_DIR}/CascLib/CMakeLists.txt")
    message(FATAL_ERROR
        "  Missing dependency: CascLib\n"
        "    D2RCascCLI requires the CascLib library.\n"
        "    It is provided as a GIT submodule of this repository.\n"
        "    Did you forget to execute the following commands?\n"
        "      git submodule init\n"
        "      git submodule update\n"
        "      If you did, try re-cloning the repository with --recursive.\n"
        "    PATH: ${CMAKE_SOURCE_DIR}/CascLib/CMakeLists.txt"
    )
endif()

# Include directories for CascLib and CASCExtractor
include_directories("${CMAKE_SOURCE_DIR}/CascLib/src/"
                    "${CMAKE_SOURCE_DIR}/include/")

# List all possible library files
set(CASC_LIBRARIES
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibDAD.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibDAS.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibDUD.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibDUS.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibRAD.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibRAS.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibRUD.lib"
    "${CMAKE_SOURCE_DIR}/CascLib/build/lib/Release/x64/CascLibRUS.lib"
)

# Declare the executable and link with the selected CascLib library
add_executable(D2RCascCLI main.cpp)

# Link all possible CascLib libraries
target_link_libraries(D2RCascCLI ${CASC_LIBRARIES})

# Enable optimizations for release builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")
endif()