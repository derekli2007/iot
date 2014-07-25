//
//  GT202RemoteControlViewController.m
//  GT202
//
//  Created by Edmond Leung on 25/5/14.
//  Copyright (c) 2014 Arrow. All rights reserved.
//

#import "GT202RemoteControlViewController.h"

@interface GT202RemoteControlViewController ()
@end

@implementation GT202RemoteControlViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    for (int i=1; i <= 27; i++) {
        [(UIButton *)[self.view viewWithTag:i] addTarget:self action:@selector(onClickButton:) forControlEvents:UIControlEventTouchUpInside];
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/
-(void)onClickButton:(UIButton *)sender {
    [_home remoteControlCmd:[sender currentTitle]];
}


@end
