/*
The SmartThings end of an integration to pull weather information from an instance of weewx (http://weewx.com/)

Copyright 2017 Les Niles Les@2pi.org
This is don't-be-a-dick software.  Anyone is free to use and/or modify it for any purpose, at your own risk,
as long as this statement and license remain intact, no other license is claimed, and no other restrictions are
imposed on other users of the software.

No beer, free or otherwise, was harmed in the development of this software.
--------------------------------------------------------------------------
*/

  
metadata {
  definition(name:"Weewx Device", namespace:"lniles77", author: "Les Niles") {
    capability "Temperature Measurement"
    capability "Sensor"
    capability "Refresh"
    capability "Polling"

    attribute "observationTime", "string"
    attribute "insideTemperature", "number"
    attribute "windchill", "number"
    attribute "heatindex", "number"
    attribute "dewpoint", "number"
    attribute "humidity", "number"
    attribute "insideHumidity", "number"
    attribute "barometer", "number"
    attribute "barometerTrendData", "number"
    attribute "wind", "number"
    attribute "windDir", "number"
    attribute "windVec", "string"
    attribute "gust", "number"
    attribute "gustDir", "number"
    attribute "gustVec", "string"

  }

  tiles {
    valueTile("observationTime", "device.obsTime", width:2, height:1) {
      state "default", label:'${currentValue}'
    }

    valueTile("temperature", "device.temperatureString", width:1, height:1) {
      state "temperature", label:'${currentValue}', unit: "°F"
    }

    valueTile("windVec", "device.windVec", inactiveLabel:false) {
      state "default", label:'Wind ${currentValue}'
    }

    valueTile("gustVec", "device.gustVec", inactiveLabel:false) {
      state "default", label:'Gust ${currentValue}'
    }

    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
    }

    main "temperature"
    details(["observationTime", "temperature", "windVec", "gustVec", "refresh"])
  }
}

/* Each entry in dataFields is an array 
 */
def dataFields = [ time: [ hasUnits: false, eventName: "observationTime", makeString: false ],
		   outTemp: [ hasUnits: true, eventName: "temperature", makeString: true ],
		   inTemp: [ hasUnits: true, eventName: "insideTemperature", makeString: true ],
		   windchill: [ hasUnits: true, eventName: "windchill", makeString: true ],
		   heatindex: [ hasUnits: true, eventName: "heatindex", makeString: true ],
		   dewpoint: [ hasUnits: true, eventName: "dewpoint", makeString: true ],
		   humidity: [ hasUnits: false, eventName: "humidity", makeString: true ],
		   insideHumidity: [ hasUnits: false, eventName: "insideHumidity", makeString: true ],
		   barometer: [ hasUnits: true, eventName: "barometer", makeString: true ],
		   barometerTrendData: [ hasUnits: false, eventName: "barometerTrendData", makeString: true ]
		 ]

def setDisplayed(flags) {
}

def setServer(ip, port, url) {
  def existingIp = getDataValue("ip")
  def existingPort = getDataValue("port")
  def existingUrl = getDataValue("jsonPath")

  log.debug "setServer(${ip}, ${port}, ${url})"

  if ( existingIp && ip == existingIp
       && existingPort && port == existingPort
       && existingUrl && url == existingUrl ) {
    log.debug "no change to server"
  } else {
    updateDataValue("port", port.toString())
    updateDataValue("ip", ip)
    updateDataValue("jsonPath", url)
  }

  log.debug "setServer done, DNI=${device.deviceNetworkId}"
  return ""
}


String[] attributeNames = []
  
// Parse the response
def parse(description) {
  log.debug "parsing weewx data ${description}"

  def msg = parseLanMessage(description)

  def data = msg.json

  log.debug "dataFields are ${dataFields}"
  if ( attributeNames.size == 0 ) {
    getSupportedAttributes().each {
      attributeNames.add(it.getName())
    }
  }
  log.debug "Attributes are ${attributeNames}"
  if ( data ) {

    dataFields.keySet()?.each {

      if ( data.containsKey(it) && data[it] != null ) {
	def ev = dataFields[it]["eventName"]
	def val = data[it]
	def unName = "${it}Units".trim()

	log.debug "${it} ${ev} = ${val}"
	sendEvent(name: ev, value: val)

	if ( data.containsKey(unName) && data[unName] != null ) {
	  def units = data[unName]
	  log.debug "units for ${ev} = ${units}"
	  if ( attributeNames.contains(ev) ) {
	    sendEvent(name: "${ev}Units", value: units)
	    log.debug "event ${ev}Units = ${units}"
	  }
	  if ( dataFields[it]["makeString"] && attributeNames.contains("${ev}String") ) {
	    sendEvent(name: "${ev}String", value: "${val}${units}")
	    log.debug "event ${ev}String = ${val}${units}"
	  }
	}
      }
    }
      
    if ( data.containsKey("time") && data.time ) {
      log.debug "Observation Time ${data.time}"
      sendEvent(name: "obsTime", value: data.time)
    }

    if ( data.containsKey("outTemp") && data.outTemp != Null ) {
      log.debug "temperature ${data.outTemp}"
      sendEvent(name: "temperature", value: data.outTemp as float)
      if ( data.containsKey("outTempUnits") && data.outTempUnits != Null ) {
	sendEvent(name: "temperatureUnits", value: data.outTempUnits)
	sendEvent(name: "temperatureString", value: "${data.outTemp}${data.outTempUnits}")
      }
    }

    if ( data.containsKey("windSpeed") && data.windSpeed != Null ) {
      log.debug "wind ${data.windSpeed}"
      sendEvent(name: "wind", value: data.windSpeed as float)

      if ( data.containsKey("windSpeedUnits") && data.windSpeedUnits != Null ) {
	sendEvent(name: "windSpeedUnits", value: data.windSpeedUnits)

	if ( data.containsKey("windDir") && data.windDir != Null ) {
	  log.debug "windDir ${data.windDir}"
	  sendEvent(name: "windDir", value: data.windDir as float)

	  def windv = "calm"
	  if ( data.windSpeed > 0.01 ) {
	    windv = "${data.windSpeed}${data.windSpeedUnits} from ${data.windDir}°"
	  }
	  sendEvent(name: "windVec", value: windv)
	  log.debug "windVec ${windv}"
	}
      }
    }


    if ( data.containsKey("windGust") && data.windGust != Null ) {
      log.debug "gust ${data.windGust}"
      sendEvent(name: "gust", value: data.windGust as float)

      if ( data.containsKey("windSpeedUnits") && data.windSpeedUnits != Null ) {
	if ( data.containsKey("windGustDir") && data.windGustDir != Null ) {
	  log.debug "gustDir ${data.windGustDir}"
	  sendEvent(name: "gustDir", value: data.windGustDir as float)

	  def gustv = "-"
	  if ( data.windGust > 0.01 ) {
	    gustv = "${data.windGust}${data.windSpeedUnits} from ${data.windGustDir}°"
	  }
	  sendEvent(name: "gustVec", value: gustv)
	  log.debug "gustVec ${gustv}"
	}
      }
    }

  }
}

def poll() {
  log.debug "Polling\n"
  getWeewxData()
}

def refresh() {
  log.debug "Refreshing\n"
  getWeewxData()
}

// 

private getWeewxData() {
  log.debug "getWeewxData"
  
  def ip = getDataValue("ip")
  def port = getDataValue("port")
  def jsonPath = getDataValue("jsonPath")
  
  def params = [ method: "GET",
		 headers: [ Host: "${ip}:${port}" ],
		 path: jsonPath,
		 HOST: "${ip}:${port}"
	       ]

  log.debug "HubAction params = ${params}."

  def hubAct = new physicalgraph.device.HubAction( params, device.deviceNetworkId)
  log.debug "getWeewxData: ${params}, dni ${device.deviceNetworkId}"

  hubAct
}


