# Fizzim GUI build and test helper

ifneq ($(strip $(JAVA_HOME)),)
JAVAC ?= $(JAVA_HOME)/bin/javac
JAR ?= $(JAVA_HOME)/bin/jar
else
JAVAC ?= javac
JAR ?= jar
endif

BASH ?= bash
SRC_DIR ?= src
RESOURCE_DIR ?= resources
LIB_DIR ?= lib
BUILD_DIR ?= build
CLASS_DIR ?= $(BUILD_DIR)/classes
JAVA_SOURCES := $(wildcard $(SRC_DIR)/*.java)
JAR_NAME ?= fizzim.jar
MANIFEST ?= manifest.txt
JAVA_RELEASE ?= 11
RESOURCE_FILES := $(wildcard $(RESOURCE_DIR)/*)
VENDORED_CLASSES := $(wildcard $(LIB_DIR)/org/jdesktop/layout/*.class)

.PHONY: all jar clean test test-verilog test-fuzz help

all: jar

jar: $(JAR_NAME)

$(JAR_NAME): $(JAVA_SOURCES) $(MANIFEST) $(RESOURCE_FILES) $(VENDORED_CLASSES)
	mkdir -p $(CLASS_DIR)
	$(JAVAC) --release $(JAVA_RELEASE) -cp $(LIB_DIR) -d $(CLASS_DIR) $(JAVA_SOURCES)
	$(JAR) cfm $(JAR_NAME) $(MANIFEST) -C $(CLASS_DIR) . -C $(RESOURCE_DIR) . -C $(LIB_DIR) org

clean:
	rm -rf $(BUILD_DIR)
	rm -f *.jar jar.log

test: test-verilog

test-verilog:
	$(BASH) testcases/run_backend_flow.sh

test-fuzz: jar
	node testcases/tools/fuzz_backend_compare.js

help:
	@echo "Fizzim GUI build:"
	@echo "  make              Build $(JAR_NAME)"
	@echo "  make jar          Build $(JAR_NAME)"
	@echo "  make clean        Remove generated Java build artifacts"
	@echo "  make test         Run public Verilog/backend regression"
	@echo "  make test-verilog Run public Verilog/backend regression"
	@echo "  make test-fuzz    Run randomized Perl/Java backend comparison fuzzing"
	@echo ""
	@echo "Variables:"
	@echo "  JAVA_HOME=/path/to/jdk"
	@echo "  JAVA_RELEASE=11"
	@echo "  JAVAC=/path/javac JAR=/path/jar BASH=/path/bash JAR_NAME=fizzim.jar"
	@echo "  SRC_DIR=src RESOURCE_DIR=resources LIB_DIR=lib BUILD_DIR=build"
	@echo "  OSS_CAD_SUITE=/path/to/oss-cad-suite PERL_BIN=/path/perl"
