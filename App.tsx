import React, { useEffect, useState } from 'react';
import { View, Text, NativeEventEmitter, Button } from 'react-native';
// import getLocation from './LocationModule'; // import the function
import { NativeModules } from 'react-native';
import { PERMISSIONS, check, request } from 'react-native-permissions';


const { LocationModule, SplashModule } = NativeModules;
console.log('NativeModules: ', SplashModule);

const locationEmitter = new NativeEventEmitter(LocationModule);


const App = () => {


  const [loc, setLoc] = useState({})
  const [err, setErr] = useState("")
  const [updatedAt, setUpdatedAt] = useState("")

  useEffect(() => {
    SplashModule.hide()
    // getPermission()
    // getLocation();
  }, []);

  const getPermission = async () => {

    try {
      const isLocationEnabled = await LocationModule.checkAndRequestLocation();
      if (isLocationEnabled) {
        getLocation()
      } else {
        setErr('User denied enabling location')
      }
    } catch (error) {
      setErr('Error checking location settings: ' + error);
    }

  }

  const getLocation = async () => {
    try {
      setErr("")
      setLoc("")
      request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION)
        .then(async result => {
          if (result === "granted") {
            console.log('result: ', result);

            // Start listening for location updates
            locationSubscribe()
          }

        })

    } catch (error) {
      console.log('Location Error: ', error);
    }
  };

  function locationSubscribe() {
    LocationModule.startLocationUpdates();

    const locationSubscription = locationEmitter.addListener(
      'locationUpdated',
      (location) => {
        console.log('Location Updated:', location);
        setLoc(location)
        setUpdatedAt(new Date().toTimeString())
      }
    );

    const errorSubscription = locationEmitter.addListener(
      'locationError',
      (error) => {
        console.log('Location Error:', error.errorMessage);
        setErr(error.errorMessage)
      }
    );

    // Clean up the listener on component unmount
    return () => {
      locationSubscription.remove();
      errorSubscription.remove();
      LocationModule.stopLocationUpdates();
    };
  }

  return (
    <View  style={{flex:1,backgroundColor:"#fff"}} >
      <Text style={txtStyle} >Getting Location...</Text>
      <Text style={{ ...txtStyle, color: "red" }} >Err: {err}</Text>
      <Text style={txtStyle} >Updated At: {updatedAt}</Text>
      <Text style={txtStyle} >Loc:{JSON.stringify(loc)}</Text>
      <Button onPress={getPermission} title='Reload' />
    </View>
  );
};

export default App;


const txtStyle = {
  fontSize: 25
}