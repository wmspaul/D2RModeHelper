cmake_minimum_required(VERSION 3.5)

project(CascLib)

# Define header and source files
set(HEADER_FILES
    src/CascCommon.h
    src/CascLib.h
    src/CascMndx.h
    src/CascPort.h
    src/common/Common.h
    src/common/FileStream.h
    src/common/ListFile.h
    src/common/Map.h
    src/jenkins/lookup.h
)

set(SRC_FILES
    src/common/Common.cpp
    src/common/Directory.cpp
    src/common/DumpContext.cpp
    src/common/DynamicArray.cpp
    src/common/FileStream.cpp
    src/common/ListFile.cpp
    src/common/Map.cpp
    src/common/RootHandler.cpp
    src/jenkins/lookup3.c
    src/CascCommon.cpp
    src/CascDecompress.cpp
    src/CascDecrypt.cpp
    src/CascDumpData.cpp
    src/CascFiles.cpp
    src/CascFindFile.cpp
    src/CascOpenFile.cpp
    src/CascOpenStorage.cpp
    src/CascReadFile.cpp
    src/CascRootFile_Diablo3.cpp
    src/CascRootFile_Mndx.cpp
    src/CascRootFile_Ovr.cpp
    src/CascRootFile_WoW6.cpp
)

set(TOMCRYPT_FILES
    src/libtomcrypt/src/hashes/hash_memory.c
    src/libtomcrypt/src/hashes/md5.c
    src/libtomcrypt/src/misc/crypt_argchk.c
    src/libtomcrypt/src/misc/crypt_hash_descriptor.c
    src/libtomcrypt/src/misc/crypt_hash_is_valid.c
    src/libtomcrypt/src/misc/crypt_libc.c
)

set(ZLIB_BZIP2_FILES
    src/zlib/adler32.c
    src/zlib/crc32.c
    src/zlib/inffast.c
    src/zlib/inflate.c
    src/zlib/inftrees.c
    src/zlib/zutil.c
)

# Add the necessary definitions based on build configuration
add_definitions(-D_7ZIP_ST -DWIN32)

# Platform-specific settings
if(WIN32)
    set(SRC_ADDITIONAL_FILES ${ZLIB_BZIP2_FILES} ${TOMCRYPT_FILES})
    set(LINK_LIBS wininet)
elseif(APPLE)
    message(STATUS "Using Mac OS X port")
    set(LINK_LIBS z bz2)
    set(SRC_ADDITIONAL_FILES ${TOMCRYPT_FILES})
elseif(${CMAKE_SYSTEM_NAME} STREQUAL Linux)
    message(STATUS "Using Linux port")
    option(WITH_LIBTOMCRYPT "Use system LibTomCrypt library" OFF)
    if(WITH_LIBTOMCRYPT)
        set(LINK_LIBS z bz2 tomcrypt)
    else()
        set(LINK_LIBS z bz2)
        set(SRC_ADDITIONAL_FILES ${TOMCRYPT_FILES})
    endif()
endif()

# Define the build configurations for Debug/Release, Static/Dynamic CRT, Unicode/Ansi
set(CONFIGURATIONS
    "DebugAD" "Debug Ansi Dynamic"
    "DebugAS" "Debug Ansi Static"
    "DebugUD" "Debug Unicode Dynamic"
    "DebugUS" "Debug Unicode Static"
    "ReleaseAD" "Release Ansi Dynamic"
    "ReleaseAS" "Release Ansi Static"
    "ReleaseUD" "Release Unicode Dynamic"
    "ReleaseUS" "Release Unicode Static"
)

# Ensure unique target names and avoid adding duplicate libraries
foreach(CONFIGURATION ${CONFIGURATIONS})
    # Create a valid library name for each configuration
    string(REPLACE " " "" CONFIG_NAME_SANITIZED ${CONFIGURATION})
    string(REPLACE "Debug" "D" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})
    string(REPLACE "Release" "R" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})
    string(REPLACE "Ansi" "A" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})
    string(REPLACE "Unicode" "U" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})
    string(REPLACE "Dynamic" "D" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})
    string(REPLACE "Static" "S" CONFIG_NAME_SANITIZED ${CONFIG_NAME_SANITIZED})

    # Check if the library already exists, to prevent adding duplicates
    if (NOT TARGET casc_${CONFIG_NAME_SANITIZED})
        # Set the appropriate build type
        if(CONFIGURATION MATCHES "Debug.*")
            set(CMAKE_BUILD_TYPE Debug)
        elseif(CONFIGURATION MATCHES "Release.*")
            set(CMAKE_BUILD_TYPE Release)
        endif()

        # Set character set for Unicode or Ansi
        if(CONFIGURATION MATCHES ".*Ansi.*")
            add_definitions(-D_CRT_SECURE_NO_WARNINGS)
            set(CASC_CHARSET "Ansi")
        elseif(CONFIGURATION MATCHES ".*Unicode.*")
            add_definitions(-DUNICODE -D_UNICODE)
            set(CASC_CHARSET "Unicode")
        endif()

        # Set the CRT type (Static or Dynamic)
        if(CONFIGURATION MATCHES ".*Static.*")
            set(CASC_CRT "Static")
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MT")
        elseif(CONFIGURATION MATCHES ".*Dynamic.*")
            set(CASC_CRT "Dynamic")
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MD")
        endif()

        # Create the library for this configuration
        add_library(casc_${CONFIG_NAME_SANITIZED} STATIC ${SRC_FILES} ${SRC_ADDITIONAL_FILES})
        target_link_libraries(casc_${CONFIG_NAME_SANITIZED} ${LINK_LIBS})

        # Set the output directory for the libraries and name the libraries according to the configuration
        set_target_properties(casc_${CONFIG_NAME_SANITIZED} PROPERTIES
            ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
            OUTPUT_NAME "CascLib${CONFIG_NAME_SANITIZED}"
        )
    endif()
endforeach()

# Install configuration
install(TARGETS casc_${CONFIG_NAME_SANITIZED} RUNTIME DESTINATION bin LIBRARY DESTINATION lib ARCHIVE DESTINATION lib FRAMEWORK DESTINATION /Library/Frameworks)
install(FILES src/CascLib.h src/CascPort.h DESTINATION include)
