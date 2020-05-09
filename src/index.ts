/* eslint-disable @typescript-eslint/no-use-before-define */
import { NativeModules, Platform } from 'react-native';
import { Config, JsonRequestParams, UploadParams } from './types';

const { Netify } = NativeModules;

const defaultConfig: Config = {
  timeout: 60000, // 60s
};

function init(params?: Config) {
  Netify.init({
    ...defaultConfig,
    ...params,
  });
}

async function jsonRequest(params: JsonRequestParams) {
  try {
    let { url, body } = params;
    url = url.replace(/([^:])(\/\/+)/g, '$1/');
    if (body && Platform.OS === 'android') {
      body = JSON.stringify(body);
    }
    const response = await Netify.jsonRequest({ ...params, url, body });
    return response;
  } catch (error) {
    const { userInfo } = error;
    if (userInfo) {
      const { response } = userInfo;
      error.response = response;
      throw error;
    }
    throw error;
  }
}

export default {
  init,
  jsonRequest,
};

export * from './types';
