param=""
for i in $*
do
param=$param" "$i
done
echo "Command line parameter list..."
echo $param
cd target
java -Xdock:icon=../images/icon.JPG -cp . -Xmx6g -Dsun.java2d.d3d=false -jar commandcenter-0.0.1-SNAPSHOT.jar $param &


