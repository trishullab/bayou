#python ../../bayou/src/main/python/bayou/experiments/2dviz/plot.py ../../Corpus/rawData/DATA-extracted-for-CACM-train.json --save save/
export PYTHONPATH=~/Research/Bayou/bayou/src/main/python
folder=$1
python ../../bayou/src/main/python/bayou/models/low_level_evidences/test.py ./../Corpus/rawData/DATA-extracted-for-CACM-train.json --save save

cd plots
mkdir $folder
rm *.gv
mv *.eps $folder/

cd $folder

gs -q -dNOPAUSE -dBATCH -sDEVICE=pdfwrite -dEPSCrop -sOutputFile=$1.pdf *.eps

rm *.eps

cd ..
cd ..

