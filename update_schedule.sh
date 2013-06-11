randir=$PWD
echo $randir
echo "Please enter your njtransit.com developer username:"
read username
echo "Please enter your njtransit.com developer password:"
read -s password
cd pc/trainapp/gtfs/
curl -c cookies.txt -o /dev/null -L "http://www.njtransit.com"
curl -c cookies.txt -o /dev/null -b cookies.txt -L "https://www.njtransit.com/developer"
curl -b cookies.txt -o /dev/null -b cookies.txt -L -d "userName=$username&password=$password" "https://www.njtransit.com/mt/mt_servlet.srv?hdnPageAction=MTDevLoginSubmitTo"
curl -b cookies.txt -o /dev/null -c cookies.txt -L "https://www.njtransit.com/mt/mt_servlet.srv?hdnPageAction=MTDevResourceDownloadTo&Category=rail" -o njtransit.zip
unzip -d njtransit -o njtransit.zip
cd ..
python graph_builder.py
cd target
split -a 3 -b 125k test.db database_ 
cd "$randir/android/res/raw"
FILES="database*"
for f in $FILES
do
  echo "Processing $f file..."
  # take action on each file. $f store current file name
  rm -f "$f"
done
cd "$randir/pc/trainapp/target"
FILES="database_*"
for f in $FILES
do
  cp $f "$randir/android/res/raw/"
done
cd $randir