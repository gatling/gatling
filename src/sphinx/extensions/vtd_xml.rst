#######
VTD XML
#######

The default XPath check used by Gatling is based on Jaxen and Xerces.
Like most XPath implementations, it loads a DOM tree of the document in memory. This leads to big memory usage which you might want to avoid if you want to use XPath checks thourough your scenarios.

To palliate this, we wrote this plugin using VTD-XML, an XML parsing library that still loads the XML document in memory; but instead of creating the DOM, it creates index, leading to big memory savings.

VTD-XML check plugin is available for download `here <http://goo.gl/6KdrK>`__.

The VTD-XML binaries can be downloaded `here <http://vtd-xml.sourceforge.net>`__.

The source is hosted `on Github <https://github.com/excilys/gatling-vtd/>`__ and published under GPLv2.