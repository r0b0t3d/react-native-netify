/* eslint-disable @typescript-eslint/no-use-before-define */
import { NativeModules, Platform } from 'react-native';

const { Netify } = NativeModules;
const isIos = Platform.OS === 'ios';

const defaultConfig: Config = {
  timeout: 60,
};

function init(params?: Config) {
  Netify.init({
    ...defaultConfig,
    ...params,
  });
}

async function jsonRequest(params: JsonRequestParams) {
  try {
    let { url } = params;
    url = url.replace(/([^:])(\/\/+)/g, '$1/');
    const response = await Netify.jsonRequest({ ...params, url });
    return response;
  } catch (error) {
    if (isIos) {
      handleIosError(error);
    }
    handleAndroidError(error);
  }
}

function handleAndroidError(error: any) {
  console.log('Error', JSON.stringify(error));
  const { code, userInfo } = error;
  if (userInfo) {
    const { response } = userInfo;
    error.response = response;
    throw error;
  }
  throw error;
}

function handleIosError(error: any) {
  if (error.userInfo) {
    const { response } = error.userInfo;
    const err = new Error(error.message);
    // @ts-ignore
    err.response = response;
    throw err;
  }
}

export default {
  init,
  jsonRequest,
};
