type Config = {
  /**
   * Set timeout for request in milliseconds
   */
  timeout?: number;
};

type Method = 'get' | 'post' | 'put' | 'patch' | 'delete';

type JsonRequestParams = {
  url: string;
  method: Method;
  headers?: { [key: string]: string };
  body: any;
};
