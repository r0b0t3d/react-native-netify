//
//  RCTConvert+NetifyMethod.m
//  Netify
//
//  Created by Tuan Luong on 4/25/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTConvert.h>

typedef NS_ENUM(NSInteger, NetifyMethod) {
  GET,
  POST,
  PATCH,
  DELETE,
  PUT
};

@implementation RCTConvert (NetifyMethod)

RCT_ENUM_CONVERTER(NetifyMethod, (@{ @"get": @(GET),
                                     @"post": @(POST),
                                     @"patch": @(PATCH),
                                     @"delete": @(DELETE),
                                     @"put": @(PUT)
                                  }), GET, integerValue);

@end
