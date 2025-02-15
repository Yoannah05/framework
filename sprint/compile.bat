@echo off 
set "CLASSES_DIR=bin"
set "JAR_FILE=sprint.jar"

jar cvf %JAR_FILE% -C %CLASSES_DIR% .

xcopy /s /q /y "sprint.jar" "/home/yoannah/Documents/ITU/framework/sprint/lib"

rm "sprint.jar"