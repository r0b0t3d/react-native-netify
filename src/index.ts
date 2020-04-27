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
    return await Netify.jsonRequest(params);
  } catch (error) {
    if (isIos) {
      handleIosError(error);
    }
    throw error;
  }
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
