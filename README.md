# react-native-netify

Native networking library for react native. Use [AFNetworking](https://github.com/AFNetworking/AFNetworking) for iOS and [FastAndroidNetworking](https://github.com/amitshekhariitbhu/Fast-Android-Networking) for Android.

## Getting started

`$ yarn add react-native-netify`

## Usage

```javascript
import Netify from 'react-native-netify';

// Init module
Netify.init({
    // Set timeout for request
    timeout: 60000,
});

Netify.jsonRequest({
    url: apiUrl,
    method: 'post',
    headers: {
        'Content-Type': 'application/json'
    },
    body: {
        name: userName,
    }
})
.then(response => console.log(response))
.catch(error => {
    const { code, message, response } = error;
    console.log(code, message, response);
});
```
