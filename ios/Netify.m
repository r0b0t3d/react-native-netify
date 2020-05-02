#import "Netify.h"

#import <AFNetworking/AFNetworking.h>
#import <React/RCTConvert.h>

@interface NetifyJSONRequestSerializer : AFJSONRequestSerializer

@property (nonatomic, assign) NSTimeInterval timeout;
- (id)initWithTimeout:(NSTimeInterval)timeout;

@end

@implementation NetifyJSONRequestSerializer
- (id)initWithTimeout:(NSTimeInterval)timeout {
  self = [super init];
  if (self) {
    self.timeout = timeout;
  }
  return self;
}

- (NSMutableURLRequest *)requestWithMethod:(NSString *)method URLString:(NSString *)URLString parameters:(id)parameters error:(NSError * _Nullable __autoreleasing *)error {
  NSMutableURLRequest *request = [super requestWithMethod:method URLString:URLString parameters:parameters error:error];
  
  if (self.timeout > 0) {
    [request setTimeoutInterval:self.timeout];
  }
  return request;
  
}

@end

@implementation Netify {
  int timeout;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(init:(NSDictionary*)params)
{
  timeout = [RCTConvert int:params[@"timeout"]] ?: 60;
}

RCT_EXPORT_METHOD(jsonRequest:(NSDictionary *)params
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSString* url = [RCTConvert NSString:params[@"url"]];
  NSDictionary* body = [RCTConvert NSDictionary:params[@"body"]];
  NSString* method = [RCTConvert NSString:params[@"method"]];
  NSDictionary* headers = [RCTConvert NSDictionary:params[@"headers"]];
  AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
  manager.responseSerializer = [AFJSONResponseSerializer serializer];
  manager.responseSerializer.acceptableContentTypes = nil;
  manager.requestSerializer = [[NetifyJSONRequestSerializer alloc] initWithTimeout:timeout];
  
  // Tell application keep executing the request in background for amout of time
  UIApplication *application = [UIApplication sharedApplication];
  __block UIBackgroundTaskIdentifier bgTask = [application beginBackgroundTaskWithName:@"jsonRequest" expirationHandler:^{
    // Clean up any unfinished task business by marking where you
    // stopped or ending the task outright.
    [application endBackgroundTask:bgTask];
    bgTask = UIBackgroundTaskInvalid;
  }];
  
  NSURLSessionDataTask *dataTask = [manager dataTaskWithHTTPMethod:[method uppercaseString]
                                                         URLString:url
                                                        parameters:body
                                                           headers:headers
                                                    uploadProgress:^(NSProgress * _Nonnull uploadProgress) {
    NSLog(@"uploadProgress %lld", uploadProgress.totalUnitCount / uploadProgress.completedUnitCount);
  }
                                                  downloadProgress:^(NSProgress * _Nonnull downloadProgress) {
    NSLog(@"downloadProgress %lld", downloadProgress.totalUnitCount / downloadProgress.completedUnitCount);
  }
                                                           success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
    [self handleResponse:responseObject bgTask:bgTask resolver:resolve];
  }
                                                           failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
    [self handleError:task withError:error bgTask:bgTask rejecter:reject];
  }];
  [dataTask resume];
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
    case -1001:
      code = @"timeout";
      break;
    default:
      code = [@(error.code) stringValue];
      break;
  }
  if (task != nil && [task.response isKindOfClass:[NSHTTPURLResponse class]]) {
    NSInteger statusCode = ((NSHTTPURLResponse*)task.response).statusCode;
    id response = error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey];
    NSDictionary *json;
    if (response) {
      json = [NSJSONSerialization JSONObjectWithData:response
                                      options:NSJSONReadingAllowFragments
                                        error:nil];
    }
    NSDictionary* headers = ((NSHTTPURLResponse*)task.response).allHeaderFields;
    NSMutableDictionary *userInfo = [error.userInfo mutableCopy];
    userInfo[@"response"] = @{
      @"status": @(statusCode),
      @"headers": headers,
      @"data": json ?: [NSNull null]
    };
    NSError* newError = [NSError errorWithDomain:error.domain
                                            code:error.code
                                        userInfo:userInfo];
    reject(code, message, newError);
    return;
  }
  reject(code, message, error);
}

@end
