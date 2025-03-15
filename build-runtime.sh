#!/bin/sh
set -eu

JDK_PATH="jdk/bellsoft-jdk11.0.22+12-windows-amd64-full"

jlink --no-header-files \
    --no-man-pages \
    --compress=2 \
    --strip-debug \
    --add-modules java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.sql,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.web,jdk.attach,jdk.jdi,jdk.jsobject,jdk.unsupported,jdk.crypto.ec \
    --module-path "${JDK_PATH}/jmods" \
    --output win-runtime
