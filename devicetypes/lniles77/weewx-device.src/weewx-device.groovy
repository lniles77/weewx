/*
The SmartThings end of an integration to pull weather information from an instance of weewx (http://weewx.com/)

Copyright 2017 Les Niles Les@2pi.org
This is don't-be-a-dick software.  Anyone is free to use and/or modify it for any purpose, at your own risk,
as long as this statement and license remain intact, no other license is claimed, and no other restrictions are
imposed on other users of the software.

No beer, free or otherwise, was harmed in the development of this software.

*/

metadata {
  definition(name:"Weewx Device", namespace:"lniles77", author: "Les Niles") {
    capability "Temperature Measurement"
    capability "Sensor"
    capability "Refresh"
    capability "Polling"

    attribute "obsTime", "string"
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

    valueTile("temperature", "device.temperature", width:1, height:1) {
      state "temperature", label:'${currentValue}°F', unit: "°F"
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
  
    // try {
    //   log.debug "Trying http://${ip}:${port}${url}"
    //   httpGet("http://${ip}:${port}${url}") { resp ->
    // 	stat = resp.getStatus()
    // 	if ( stat != 200 ) {
    // 	  log.debug "Failed: status ${stat}"
    // 	  return "status ${stat}"
    // 	}
    // 	log.debug "response data: ${resp.data}"
    //   }
    // } catch (e) {
    //   log.error "Exception in setServer", e
    //   return "exception $e"
    // }

    updateDataValue("port", port.toString())
    updateDataValue("ip", ip)
    updateDataValue("jsonPath", url)
  }

  //  setNetworkId(ip, port)

  log.debug "setServer done, DNI=${device.deviceNetworkId}"
}

// Parse the response
def parse(description) {
  log.debug "parsing weewx data ${description}"

  def msg = parseLanMessage(description)

  def data = msg.json
  log.debug "parsed fields ${data} from ${msg}"
  
  if ( data ) {
    if ( data.containsKey("time") && data.time ) {
      log.debug "Observation Time ${data.time}"
      sendEvent(name: "obsTime", value: data.time)
    }

    if ( data.containsKey("outTemp") && data.outTemp ) {
      log.debug "temperature ${data.outTemp}"
      sendEvent(name: "temperature", value: data.outTemp as float)
    }

    if ( data.containsKey("windSpeed") && data.windSpeed ) {
      log.debug "wind ${data.windSpeed}"
      sendEvent(name: "wind", value: data.windSpeed as float)
      if ( data.containsKey("windDir") && data.windDir ) {
	log.debug "windVec"
	sendEvent(name: "windVec", value: "${data.windSpeed}mph from ${data.windDir}°")
      }
    }

    if ( data.containsKey("windDir") && data.windDir ) {
      log.debug "windDir ${data.windDir}"
      sendEvent(name: "windDir", value: data.windDir as float)
    }

    if ( data.containsKey("windGust") && data.windGust ) {
      log.debug "gust ${data.windGust}"
      sendEvent(name: "gust", value: data.windGust as float)
    }

    if ( data.containsKey("windGustDir") && data.windGustDir ) {
      log.debug "gustDir ${data.windGustDir}"
      sendEvent(name: "gustDir", value: data.windGustDir as float)
      if ( data.containsKey("windGust") && data.windGust ) {
	log.debug "gustVec"
	sendEvent(name: "gustVec", value: "${data.windGust}mph from ${data.windGustDir}°")
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


private setNetworkId(ip, port) {
  log.debug "setting Device Network ID"

  String ah = ip.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join()
  String ph = port.toString().format('%04x', port.toInteger())

  device.deviceNetworkId = "$ah:$ph".toUpperCase()

  log.debug "Device Network ID is ${device.deviceNetworkId}"
}

    
