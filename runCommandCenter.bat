echo off
set param=
for %%x in (%*) do call set "param=%%param%% %%x"
cd target 
java -cp . -Xmx3g -jar commandcenter-0.0.1-SNAPSHOT.jar %param%
cd ..

