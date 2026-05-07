# Fizzim GUI build and test helper

ifneq ($(strip $(JAVA_HOME)),)
JAVAC ?= $(JAVA_HOME)/bin/javac
JAR ?= $(JAVA_HOME)/bin/jar
else
JAVAC ?= javac
JAR ?= jar
endif

BASH ?= bash
JAVA_SOURCES := $(wildcard *.java)
CLASS_FILES := $(patsubst %.java,%.class,$(JAVA_SOURCES))
JAR_NAME ?= fizzim.jar
MANIFEST ?= manifest.txt
JAR_ASSETS := splash.png icon.png org

.PHONY: all jar clean test test-verilog test-production help

all: jar

jar: $(JAR_NAME)

$(JAR_NAME): $(JAVA_SOURCES) $(MANIFEST) splash.png icon.png
	$(JAVAC) $(JAVA_SOURCES)
	$(JAR) cfm $(JAR_NAME) $(MANIFEST) *.class $(JAR_ASSETS)

clean:
	rm -f *.class *.jar jar.log
	find . -maxdepth 1 -type d -name '*_jar' -exec rm -rf {} +

test: test-verilog

test-verilog:
	$(BASH) testcases/run_backend_flow.sh

test-production:
	$(BASH) testcases_production/run_backend_flow.sh

help:
	@echo "Fizzim GUI build:"
	@echo "  make              Build $(JAR_NAME)"
	@echo "  make jar          Build $(JAR_NAME)"
	@echo "  make clean        Remove generated Java build artifacts"
	@echo "  make test         Run public Verilog/backend regression"
	@echo "  make test-verilog Run public Verilog/backend regression"
	@echo "  make test-production"
	@echo "                    Run local private production regression"
	@echo ""
	@echo "Variables:"
	@echo "  JAVA_HOME=/path/to/jdk"
	@echo "  JAVAC=/path/javac JAR=/path/jar BASH=/path/bash JAR_NAME=fizzim.jar"
	@echo "  OSS_CAD_SUITE=/path/to/oss-cad-suite PERL_BIN=/path/perl"
