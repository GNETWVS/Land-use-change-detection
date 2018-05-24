# Land-use-change-detection
Land-Use Change Detection using Multisensor Satellite Data

##### Инструктция по сборке
1. Установить Anaconda Python 2.7 и плагин sen2cor, добавить все в переменные среды.
Подробно:
    1. Windows 10:      https://www.youtube.com/watch?v=ryGROtiHPYI
    2. Windows 8, 7:    https://www.youtube.com/watch?v=njS1iQPIOGU
    3. Linux:           https://www.youtube.com/watch?v=U5qiqH9xSf4

2. Установить GDAL, необходимо установить версию, совметимую с MSVC 2010
Ссылка: http://www.gisinternals.com/query.html?content=filelist&file=release-1600-x64-gdal-2-2-3-mapserver-7-0-7.zip
Необхоимо установить GDAL и следующие плагины:
    1. gdal-202-1600-x64-core.msi
    2. GDAL-2.2.3.win-amd64-py2.7.msi
    3. gdal-202-1600-x64-oracle.msi
    
Указать в качестве билиотеки ../GDAL/java/gdal.jar
и директория для нативных библиотек: ../GDAL
    