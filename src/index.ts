import { NativeModules } from 'react-native';

const { Netify } = NativeModules;

const defaultConfig: Config = {
  timeout: 60,
};

function init(params?: Config) {
  Netify.init({
    ...defaultConfig,
    ...params,
  });
}

function jsonRequest(params: JsonRequestParams) {
  return Netify.jsonRequest(params);
}

export default {
  init,
  jsonRequest,
};
