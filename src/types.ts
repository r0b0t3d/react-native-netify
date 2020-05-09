export type Config = {
  /**
   * Set timeout for request in milliseconds
   */
  timeout?: number;
};

export type Method = 'get' | 'post' | 'put' | 'patch' | 'delete';

type FormDataObj = {
  name: string;
  data: any;
  fileName?: string;
  type?: string;
};

interface BaseParams {
  url: string;
  method: Method;
  headers?: { [key: string]: string };
}

export interface JsonRequestParams extends BaseParams {
  body: any;
}

export interface UploadParams extends BaseParams {
  formData: FormDataObj[];
}
