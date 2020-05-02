/* eslint-disable @typescript-eslint/no-use-before-define */
import { NativeModules } from 'react-native';

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
    let { url } = params;
    url = url.replace(/([^:])(\/\/+)/g, '$1/');
    const response = await Netify.jsonRequest({ ...params, url });
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

async function upload(params: UploadParams) {
  let { url } = params;
  url = url.replace(/([^:])(\/\/+)/g, '$1/');
  return Netify.upload({ ...params, url });
}

export default {
  init,
  jsonRequest,
  upload,
};
