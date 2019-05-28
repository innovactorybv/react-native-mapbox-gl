//
//  RCTMGLShapeSource.m
//  RCTMGL
//
//  Created by Nick Italiano on 9/19/17.
//  Copyright © 2017 Mapbox Inc. All rights reserved.
//

#import "RCTMGLShapeSource.h"
#import "RCTMGLUtils.h"

@implementation RCTMGLShapeSource

- (void)setShape:(NSString *)shape
{
    _shape = shape;
    
    if (self.source != nil) {
        MGLShapeSource *source = (MGLShapeSource *)self.source;
        [source setShape:[RCTMGLUtils shapeFromGeoJSON:_shape]];
    }
}

- (void)setImages:(NSDictionary<NSString *,NSString *> *)images
{
    _images = images;
    
    // add any possible new images that might have been added after the source was created
    if (self.source != nil) {
        [self _addRemoteImages: nil];
    }
}

- (void)setNativeImages:(NSArray<NSString *> *)nativeImages
{
    _nativeImages = nativeImages;
    
    // add any possible new images that might have been added after the source was created
    if (self.source != nil) {
        [self _addNativeImages];
    }
}

- (void)addToMap
{
    if (self.map.style == nil) {
        return;
    }
    
    if (![self _hasImages] && ![self _hasNativeImages]) {
        [super addToMap];
    } else {
        [self _addNativeImages];
        [self _addRemoteImages: ^{ [super addToMap]; }];
    }
}

- (void)removeFromMap
{
    if (self.map.style == nil) {
        return;
    }
    
    [super removeFromMap];
    
    if ([self _hasImages]) {
        NSArray<NSString *> *imageNames = _images.allKeys;
        
        for (NSString *imageName in imageNames) {
            [self.map.style removeImageForName:imageName];
        }
    }
    
    if ([self _hasNativeImages]) {
        for (NSString *imageName in _nativeImages) {
            [self.map.style removeImageForName:imageName];
        }
    }
}

- (MGLSource*)makeSource
{
    NSDictionary<MGLShapeSourceOption, id> *options = [self _getOptions];
    
    if (_shape != nil) {
        MGLShape *shape = [RCTMGLUtils shapeFromGeoJSON:_shape];
        return [[MGLShapeSource alloc] initWithIdentifier:self.id shape:shape options:options];
    }
    
    NSURL *url = [[NSURL alloc] initWithString:_url];
    return [[MGLShapeSource alloc] initWithIdentifier:self.id URL:url options:options];
}

- (NSDictionary<MGLShapeSourceOption, id>*)_getOptions
{
    NSMutableDictionary<MGLShapeSourceOption, id> *options = [[NSMutableDictionary alloc] init];
    
    if (_cluster != nil) {
        options[MGLShapeSourceOptionClustered] = [NSNumber numberWithBool:[_cluster intValue] == 1];
    }
    
    if (_clusterRadius != nil) {
        options[MGLShapeSourceOptionClusterRadius] = _clusterRadius;
    }
    
    if (_clusterMaxZoomLevel != nil) {
        options[MGLShapeSourceOptionMaximumZoomLevelForClustering] = _clusterMaxZoomLevel;
    }
    
    if (_maxZoomLevel != nil) {
        options[MGLShapeSourceOptionMaximumZoomLevel] = _maxZoomLevel;
    }
    
    if (_buffer != nil) {
        options[MGLShapeSourceOptionBuffer] = _buffer;
    }
    
    if (_tolerence != nil) {
        options[MGLShapeSourceOptionSimplificationTolerance] = _tolerence;
    }
    
    return options;
}


- (void)_addNativeImages
{
    if ([self _hasNativeImages]) {
        for (NSString *imageName in _nativeImages) {
            UIImage *foundImage = [self.map.style imageForName:imageName];
            // only add native images if they are not in the style yet (similar to [RCTMGLUtils fetchImages: style:])
            if (foundImage == nil) {
                UIImage *image = [UIImage imageNamed:imageName];
                [self.map.style setImage:image forName:imageName];
            }
        }
    }
}

- (void)_addRemoteImages:(nullable void (^)())callback
{
    [RCTMGLUtils fetchImages:_bridge style:self.map.style objects:_images callback:^{ if (callback != nil) callback(); }];
}

- (void)_addAllImagesWithCallback:(nullable void (^)())callback
{
    [self _addNativeImages];
    [self _addRemoteImages: callback];
}

- (BOOL)_hasImages
{
    return _images != nil && _images.count > 0;
}

- (BOOL)_hasNativeImages
{
    return _nativeImages != nil && _nativeImages.count > 0;
}

@end
