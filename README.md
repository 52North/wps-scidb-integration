# WPS SciDB Integration


## tl;dr

This implementation provides an connection between a
WPS<sup id="user-content-a1">[1](#user-content-f1)</sup>
and
SciDB<sup id="user-content-a2">[2](#user-content-f2)</sup>
. It allows accessing data in an SciDB instance using
WMS<sup id="user-content-a3">[3](#user-content-f3)</sup>
and
WCS<sup id="user-content-a4">[4](#user-content-f4)</sup>
operations. The access is implemented via WPS processes offered in a
transparent manner via a proxy web service.


## Description

Earth Observation (EO) datasets commonly provide scenes for particular
regions of the Earth’s surface. Each scene consists of multiple raster
satellite images representing the space at individual time stamps. A
key challenge in managing EO datasets is providing fast access to
2-dimensional coverages periodically collected. A representation concept
of 3-dimensional space-time Data Cubes and an approach for providing
dynamic and on-demand delivery of 3-dimensional subsets, increases the
interoperability and usability for EO datasets. As an extreme scenario of
subsetting, also time series of single pixels can be retrieved for a
detailed analysis or visualisation.


In a prototypical development, we extract 2-dimensional gridded pixel
data from individual satellite images of single time stamps and insert
them into a 3-dimensional space-time array using SciDB, a multidimensional
array database. On arrival of a new satellite image, we transform it’s
pixel data into a 2-dimensional SciDB binary format file and push the
data on top of the present array. Metadata, such as timestamps and meaning
of the pixel values, are persisted in an additional file-based database.
Standardized access to the data is facilitated via Web Coverage and Web
Map Services (WCS and WMS by the OGC). We support common outputformats for
geo-spatio-temporal datasets such as netCDF and GeoTIFF. While the WCS
interface provides access to the raw data used in subsequent models and
analyses, the WMS provides styled images that can easily be added to map
clients. Internally, the access organised through a Web Processing Service
that evaluates the WCS and WMS request and can be enhanced with additional
(pre-)processing functionality.


These implementations are made within the MuDak-WRM project, where a
particular goal is the development of a central data delivering service for
EO scenes and in-situ measurements at water reservoirs. The quality of a
water reservoir does not only depend on the reservoir itself, but is
influenced by the surrounding environment and the entire catchment. An
in-situ monitoring of a reservoir is detailed, but also very costly. Hence,
the MuDak-WRM project aims at identifying proxies that provide insights into
the quality of a reservoir detailed enough for a mid-range management of the
reservoir that are applicable worldwide.


## References

* Developed within the
  MuDak-WRM<sup id="user-content-a5">[5](#user-content-f5)</sup>
  project funded by the German Federal Ministry of Education and Research (BMBF)
  with in the GRoW funding measure.

* Presented at EGU 2019 with a
  poster<sup id="user-content-a6">[6](#user-content-f6)</sup>.


## Contact

* Benedik Gräler<sup id="user-content-a7">[7](#user-content-f7)</sup>
* Eike Hinderk Jürrens<sup id="user-content-a8">[8](#user-content-f8)</sup>


## Developers

* Eike Hinderk Jürrens
* Maurin Radtke


## Links

<b id="user-content-f1">1</b>: https://www.opengeospatial.org/standards/wps [↩](#user-content-a1)

<b id="user-content-f2">2</b>: https://paradigm4.atlassian.net/wiki/spaces/scidb/overview [↩](#user-content-a2)

<b id="user-content-f3">3</b>: https://www.opengeospatial.org/standards/wms [↩](#user-content-a3)

<b id="user-content-f4">4</b>: https://www.opengeospatial.org/standards/wcs [↩](#user-content-a4)

<b id="user-content-f5">5</b>: https://52north.org/references/mudak-wrm/ [↩](#user-content-a5)

<b id="user-content-f5">5</b>: https://meetingorganizer.copernicus.org/EGU2019/EGU2019-14204-2.pdf [↩](#user-content-a6)

<b id="user-content-f7">7</b>: https://github.com/BenGraeler [↩](#user-content-a7)

<b id="user-content-f8">8</b>: https://github.com/EHJ-52n/ [↩](#user-content-a8)
