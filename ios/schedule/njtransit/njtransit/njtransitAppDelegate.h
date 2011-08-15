//
//  njtransitAppDelegate.h
//  njtransit
//
//  Created by Ryan Gravener on 8/12/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

@class njtransitViewController;

@interface njtransitAppDelegate : NSObject <UIApplicationDelegate> {

}

@property (nonatomic, retain) IBOutlet UIWindow *window;

@property (nonatomic, retain) IBOutlet njtransitViewController *viewController;

@end
