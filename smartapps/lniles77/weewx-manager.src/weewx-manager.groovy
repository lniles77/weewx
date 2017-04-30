/*
The SmartThings end of an integration to pull weather information from an instance of weewx (http://weewx.com/)

Copyright 2017 Les Niles Les@2pi.org
This is don't-be-a-dick software.  Anyone is free to use and/or modify it for any purpose, at your own risk,
as long as this statement and license remain intact, no other license is claimed, and no other restrictions are
imposed on other users of the software.

No beer, free or otherwise, was harmed in the development of this software.

*/

definition(name: "Weewx Service Manager",
	   namespace: "lniles77",
	   author: "Les Niles",
	   description: "This is a Service Manager SmartApp to support importing weather data from a weewx server",
	   category: "My Apps",
	   iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	   iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	   iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("Weewx server IP address") {
    input "weewxIp", "string", title: "Weewx IP address", defaultValue: "xxx.xxx.xxx.xxx", required: true
  }
  section("Weewx server port") {
    input "weewxPort", "number", title: "Weewx port #", defaultValue: 80, required: true
  }
  section("URL to current-conditions JSON file") {
    input "weewxURL", "string", title: "JSON file URL (on weewx server)", defaultValue: "/current.json", required: true
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  // The network ID gets set in setServer()
  def theDevice = addChildDevice("lniles77", "Weewx", "0000", location.hubs[0].id)

  def err = theDevice.setServer(weewxIp, weewxPort, weewxURL)
  if ( err ) {
    log.error err
    deleteChildDevice(theDevice.deviceNetworkId)
  }
}

def unsubscribe() {
  getChildDevices()?.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}
