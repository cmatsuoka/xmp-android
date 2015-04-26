#!/bin/sh

RES_DIR=../app/src/main/res
MDPI_DIR=$RES_DIR/drawable-mdpi
HDPI_DIR=$RES_DIR/drawable-hdpi
XHDPI_DIR=$RES_DIR/drawable-xhdpi
XXHDPI_DIR=$RES_DIR/drawable-xxhdpi

for i in file folder browser list grabber; do
	echo Convert $i
	convert $i.png -resize 24x24 $MDPI_DIR/$i.png
	convert $i.png -resize 36x36 $HDPI_DIR/$i.png
	convert $i.png -resize 48x48 $XHDPI_DIR/$i.png
	convert $i.png -resize 72x72 $XXHDPI_DIR/$i.png
done
