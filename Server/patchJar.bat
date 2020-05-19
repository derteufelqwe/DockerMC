set path="G:\Programme\7-Zip\7z.exe"

rem Remove certificates from jar
%path% d target/artifacts/Server.jar META-INF/*.DSA META-INF/*.SF