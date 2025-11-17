Soil NPK Prediction Project
Data Sources & API Documentation

This document lists all external datasets and APIs used for predicting Soil NPK (Nitrogen, Phosphorus, Potassium) from soil images.
To improve prediction accuracy, we incorporate weather, climate, soil, satellite, crop, and geolocation datasets.

Weather Data (3 APIs)
1.1 OpenWeatherMap

Website: https://openweathermap.org/api

Provides real-time temperature, humidity, rainfall, wind.

1.2 WeatherAPI

Website: https://www.weatherapi.com/

Offers current weather, forecast, air quality, and astronomy data.

1.3 Meteostat (Historical Weather)

Website: https://meteostat.net/

Provides hourly/daily historical climate data for any location.
Python Example:

from meteostat import Point, Daily
Point(lat, lon)

Climate Data (4 datasets – Updated)
2.1 NASA POWER Climate Data

Website: https://power.larc.nasa.gov/

Agro-climatic parameters: temp, humidity, rainfall, solar radiation.

2.2 WorldClim Climate Dataset

Website: https://www.worldclim.org/data/

High-resolution (1 km) global climate layers:

Temperature

Precipitation

Solar radiation

2.3 NOAA GHCN (Global Historical Climate Network)

Website: https://www.ncei.noaa.gov/products

Observed, high-quality weather-station datasets.

2.4 WDCC Climate Model Dataset – CSA0hi (CMIP5)

Link: https://www.wdc-climate.de/ui/entry?acronym=CSA0hi

Provides long-term climate simulations (CMIP5, ACCESS1-0 model).
Useful for:

Long-term climate trends

Historical precipitation patterns

Background climate context

Limitations: Coarse resolution, NetCDF format, simulated data.

Geolocation (Latitude/Longitude) — 3 Sources
3.1 OpenCage Geocoding API

Website: https://opencagedata.com/api

Converts place → latitude/longitude.

3.2 Google Maps Geocoding API

Website: https://developers.google.com/maps/documentation/geocoding

Accurate, reliable coordinate lookup.

3.3 Nominatim (OpenStreetMap)

Website: https://nominatim.openstreetmap.org/

Free, open-source geocoding service.

Satellite Data (3 Major Sources)
4.1 Google Earth Engine (GEE)

Website: https://earthengine.google.com/

Datasets include:

MODIS (NDVI, LST, vegetation)

Sentinel-2 (10m resolution RGB + NIR)

Landsat 8 (surface reflectance)

4.2 NASA MODIS Satellite Data

Website: https://modis.gsfc.nasa.gov/data/

Useful for:

Vegetation indices

Land surface temperature

Soil/land conditions

4.3 ESA Sentinel-2

Website: https://www.sentinel-hub.com/explore/copernicus-data-space-ecosystem/

High-resolution (10m) multispectral imagery:

RGB

NIR

SWIR

Soil Data (3 Datasets)
5.1 ISRIC SoilGrids API

Website: https://soilgrids.org/

Global 250m soil property maps:

pH

organic carbon

sand/silt/clay

bulk density

5.2 FAO Harmonized World Soil Database (HWSD)

Website: https://www.fao.org/soils-portal/data-hub/soil-maps-and-databases/hwsd

Includes:

Soil fertility

Organic carbon

Soil texture

5.3 USDA NRCS Soil Survey (SSURGO)

Website: https://www.nrcs.usda.gov/resources/data-and-reports

Most detailed soil dataset (US-based), includes:

Soil type

Nutrient levels

Texture & moisture

Crop & Agriculture Data (3 Datasets)
6.1 FAOSTAT (Global Crop Data)

Website: https://www.fao.org/faostat/

Crop yield, production, and harvested area.

6.2 USDA Crop Data Layer

Website: https://nassgeodata.gmu.edu/CropScape/

Annual crop type classification maps.

6.3 Kaggle Agriculture Datasets

Website: https://www.kaggle.com/datasets

Soil Types Dataset (Kaggle — 4 classes, 1500+ images)
https://www.kaggle.com/datasets/jhislainematchouath/soil-types-dataset
This dataset can be used for pretraining CNN since it has high quality images. But it has no NPK values.

Crop & Soil Dataset (Kaggle — with NPK)
https://www.kaggle.com/datasets/shankarpriya2913/crop-and-soil-dataset
It has NPK values and many soil properties but no images.

Rwanda Soil Nutrient Balance (NPK)
https://zenodo.org/record/7112371
It has NPK + climate covariates. Can be used for regression practice.

References

OpenWeatherMap — https://openweathermap.org/api

WeatherAPI — https://www.weatherapi.com/

Meteostat — https://meteostat.net

NASA POWER — https://power.larc.nasa.gov/

WorldClim — https://www.worldclim.org/

NOAA GHCN — https://www.ncei.noaa.gov/

WDCC CMIP5 — https://www.wdc-climate.de

ISRIC SoilGrids — https://soilgrids.org/

FAO HWSD — https://www.fao.org/soils-portal

Sentinel-2 — https://www.sentinel-hub.com/explore/copernicus-data-space-ecosystem/

MODIS — https://modis.gsfc.nasa.gov/data/

FAOSTAT — https://www.fao.org/faostat/

Kaggle — https://www.kaggle.com/datasets


How these datasets can be used in phase-1:-

1.Soil image datasets are used to build the initial CNN for extracting visual features such as color, texture, and moisture indicators.

2.NPK tabular datasets are used to train baseline ML models (Random Forest, XGBoost, MLP) for predicting nutrient values.

3.Weather and climate APIs provide context features such as rainfall, temperature, and humidity.

4.Satellite datasets supply NDVI and SAVI indices to enhance nutrient prediction accuracy.

5.Government soil data helps calibrate predictions and enables regional tuning.

6.Crop datasets provide the basis for the preliminary fertilizer recommendation engine.