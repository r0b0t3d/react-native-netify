#import "Netify.h"

#import <AFNetworking/AFNetworking.h>
#import <React/RCTConvert.h>
#import "RCTConvert+NetifyMethod.m"

@implementation Netify {
  int timeout;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(init:(NSDictionary*)params)
{
  timeout = [RCTConvert int:params[@"timeout"]];
}

RCT_EXPORT_METHOD(jsonRequest:(NSDictionary *)params
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSString* url = [RCTConvert NSString:params[@"url"]];
  NSDictionary* body = [RCTConvert NSDictionary:params[@"body"]];
  NetifyMethod method = [RCTConvert NetifyMethod:params[@"method"]];
  AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
  manager.responseSerializer = [AFJSONResponseSerializer serializer];
  manager.responseSerializer.acceptableContentTypes = nil;
  manager.requestSerializer = [self buildRequest:params];
  
  // Tell application keep executing the request in background for amout of time
  UIApplication *application = [UIApplication sharedApplication];
  __block UIBackgroundTaskIdentifier bgTask = [application beginBackgroundTaskWithName:@"jsonRequest" expirationHandler:^{
    // Clean up any unfinished task business by marking where you
    // stopped or ending the task outright.
    [application endBackgroundTask:bgTask];
    bgTask = UIBackgroundTaskInvalid;
  }];
  switch (method) {
    case GET: {
      [manager GET:url parameters:nil progress:nil success:^(NSURLSessionTask *task, id responseObject) {
        [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
      } failure:^(NSURLSessionTask *operation, NSError *error) {
        [self handleError:operation withError:error bgTask:bgTask rejecter:reject];
      }];
      break;
    }
    case POST: {
      [manager POST:url parameters:body progress:nil success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:task withError:error bgTask:bgTask rejecter:reject];
      }];
      break;
    }
    case DELETE: {
      [manager DELETE:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:task withError:error bgTask:bgTask rejecter:reject];
      }];
      break;
    }
    case PATCH: {
      [manager PATCH:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:task withError:error bgTask:bgTask rejecter:reject];
      }];
      break;
    }
    case PUT: {
      [manager PUT:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:task withError:error bgTask:bgTask rejecter:reject];
      }];
      break;
    }
    default:
      break;
  }
}

- (void)handleResponse:(id)responseObject bgTask:(UIBackgroundTaskIdentifier)bgTask resolver:(RCTPromiseResolveBlock)resolve {
  if (bgTask != UIBackgroundTaskInvalid) {
    UIApplication *application = [UIApplication sharedApplication];
    [application endBackgroundTask:bgTask];
    bgTask = UIBackgroundTaskInvalid;
  }
  resolve(responseObject);
}

- (void)handleError:(NSURLSessionTask*)task withError:(NSError*)error bgTask:(UIBackgroundTaskIdentifier)bgTask rejecter:(RCTPromiseRejectBlock)reject {
  if (bgTask != UIBackgroundTaskInvalid) {
    UIApplication *application = [UIApplication sharedApplication];
    [application endBackgroundTask:bgTask];
    bgTask = UIBackgroundTaskInvalid;
  }
  NSString* code;
  NSString* message = error.localizedDescription;
  switch (error.code) {
    case -1004:
      code = @"network_error";
      break;
      
    default:
      code = [@(error.code) stringValue];
      break;
  }
  if (task != nil && [task.response isKindOfClass:[NSHTTPURLResponse class]]) {
    NSInteger statusCode = ((NSHTTPURLResponse*)task.response).statusCode;
    id response = error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey];
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:response
                                                         options:NSJSONReadingAllowFragments
                                                           error:nil];
    NSDictionary* headers = ((NSHTTPURLResponse*)task.response).allHeaderFields;
    NSMutableDictionary *userInfo = [error.userInfo mutableCopy];
    userInfo[@"response"] = @{
      @"status": @(statusCode),
      @"headers": headers,
      @"data": json
    };
    NSError* newError = [NSError errorWithDomain:error.domain
                                            code:error.code
                                        userInfo:userInfo];
    reject(code, message, newError);
    return;
  }
  reject(code, message, error);
}

- (AFJSONRequestSerializer*)buildRequest:(NSDictionary*) params {
  AFJSONRequestSerializer* request = [AFJSONRequestSerializer serializer];
  [request setTimeoutInterval:timeout];
  NSDictionary* headers = [RCTConvert NSDictionary:params[@"headers"]];
  if (headers != nil) {
    for (NSString* key in headers) {
      NSString* value = [RCTConvert NSString:headers[key]];
      [request setValue:value forHTTPHeaderField:key];
    }
  }
  return request;
}

@end
