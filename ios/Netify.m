#import "Netify.h"

#import <AFNetworking/AFNetworking.h>
#import <React/RCTConvert.h>
#import "RCTConvert+NetifyMethod.m"

@implementation Netify

RCT_EXPORT_MODULE()

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
  switch (method) {
    case GET: {
      [manager GET:url parameters:nil progress:nil success:^(NSURLSessionTask *task, id responseObject) {
        resolve(responseObject);
      } failure:^(NSURLSessionTask *operation, NSError *error) {
        [self handleError:error rejecter:reject];
      }];
      break;
    }
    case POST: {
      [manager POST:url parameters:body progress:nil success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        resolve(responseObject);
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:error rejecter:reject];
      }];
      break;
    }
    case DELETE: {
      [manager DELETE:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        resolve(responseObject);
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:error rejecter:reject];
      }];
      break;
    }
    case PATCH: {
      [manager PATCH:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        resolve(responseObject);
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:error rejecter:reject];
      }];
      break;
    }
    case PUT: {
      [manager PUT:url parameters:body success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        resolve(responseObject);
      } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        [self handleError:error rejecter:reject];
      }];
      break;
    }
    default:
      break;
  }
}

- (void)handleError:(NSError*)error rejecter:(RCTPromiseRejectBlock)reject {
  NSString* errResponse = [[NSString alloc] initWithData:(NSData *)error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey] encoding:NSUTF8StringEncoding];
  NSLog(@"%@",errResponse);
  reject(@"", errResponse, error);
}

- (AFJSONRequestSerializer*)buildRequest:(NSDictionary*) params {
  AFJSONRequestSerializer* request = [AFJSONRequestSerializer serializer];
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


