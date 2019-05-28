package com.mapbox.rctmgl.components.styles.sources;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.rctmgl.components.mapview.RCTMGLMapView;
import com.mapbox.rctmgl.events.FeatureClickEvent;
import com.mapbox.rctmgl.utils.DownloadMapImageTask;
import com.mapbox.services.commons.geojson.Feature;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by nickitaliano on 9/19/17.
 */

public class RCTMGLShapeSource extends RCTSource<GeoJsonSource> {
    private URL mURL;
    private RCTMGLShapeSourceManager mManager;

    private String mShape;

    private Boolean mCluster;
    private Integer mClusterRadius;
    private Integer mClusterMaxZoom;

    private Integer mMaxZoom;
    private Integer mBuffer;
    private Double mTolerance;
    private boolean mRemoved = false;

    private List<Map.Entry<String, String>> mImages;
    private List<Map.Entry<String, BitmapDrawable>> mNativeImages;

    public RCTMGLShapeSource(Context context, RCTMGLShapeSourceManager manager) {
        super(context);
        mManager = manager;
    }

    @Override
    public void addToMap(final RCTMGLMapView mapView) {
        mRemoved = false;
        if (!hasNativeImages() && !hasImages()) {
            super.addToMap(mapView);
            return;
        }

        DownloadMapImageTask.OnAllImagesLoaded imagesLoadedCallback = new DownloadMapImageTask.OnAllImagesLoaded() {
          @Override
          public void onAllImagesLoaded() {
            // don't add the ShapeSource when the it was removed while loading images
            if (mRemoved) return;
            RCTMGLShapeSource.super.addToMap(mapView);
          }
        };
        addNativeImages(mapView);
        addRemoteImages(mapView, imagesLoadedCallback);
    }

    @Override
    public void removeFromMap(RCTMGLMapView mapView) {
        super.removeFromMap(mapView);
        mRemoved = true;
        if (mMap == null) return;

        if (hasImages()) {
            for (Map.Entry<String, String> image : mImages) {
                mMap.removeImage(image.getKey());
            }
        }

        if (hasNativeImages()) {
            for (Map.Entry<String, BitmapDrawable> image : mNativeImages) {
                mMap.removeImage(image.getKey());
            }
        }
    }

    @Override
    public GeoJsonSource makeSource() {
        GeoJsonOptions options = getOptions();

        if (mShape != null) {
            return new GeoJsonSource(mID, mShape, options);
        }

        return new GeoJsonSource(mID, mURL, options);
    }

    public void setURL(URL url) {
        mURL = url;

        if (mSource != null && mMapView != null && !mMapView.isDestroyed() ) {
            ((GeoJsonSource) mSource).setUrl(mURL);
        }
    }

    public void setShape(String geoJSONStr) {
        mShape = geoJSONStr;

        if (mSource != null && mMapView != null && !mMapView.isDestroyed() ) {
            ((GeoJsonSource) mSource).setGeoJson(mShape);
        }
    }

    public void setCluster(boolean cluster) {
        mCluster = cluster;
    }

    public void setClusterRadius(int clusterRadius) {
        mClusterRadius = clusterRadius;
    }

    public void setClusterMaxZoom(int clusterMaxZoom) {
        mClusterMaxZoom = clusterMaxZoom;
    }

    public void setMaxZoom(int maxZoom) {
        mMaxZoom = maxZoom;
    }

    public void setBuffer(int buffer) {
        mBuffer = buffer;
    }

    public void setTolerance(double tolerance) {
        mTolerance = tolerance;
    }

    public void setImages(List<Map.Entry<String, String>> images) {
        mImages = images;

        // add possible new images that might have been added after the source was created
        if (mSource != null) {
          addRemoteImages(mMapView, null);
        }
    }

    public void setNativeImages(List<Map.Entry<String, BitmapDrawable>> nativeImages) {
        mNativeImages = nativeImages;

        // add any possible new images that might have been added after the source was created
        if (mSource != null) {
          addNativeImages(mMapView);
        }
    }

    public void onPress(Feature feature) {
        mManager.handleEvent(FeatureClickEvent.makeShapeSourceEvent(this, feature));
    }

    private GeoJsonOptions getOptions() {
        GeoJsonOptions options = new GeoJsonOptions();

        if (mCluster != null) {
            options.withCluster(mCluster);
        }

        if (mClusterRadius != null) {
            options.withClusterRadius(mClusterRadius);
        }

        if (mClusterMaxZoom != null) {
            options.withClusterMaxZoom(mClusterMaxZoom);
        }

        if (mMaxZoom != null) {
            options.withMaxZoom(mMaxZoom);
        }

        if (mBuffer != null) {
            options.withBuffer(mBuffer);
        }

        if (mTolerance != null) {
            options.withTolerance(mTolerance.floatValue());
        }

        return options;
    }


    private void addNativeImages(RCTMGLMapView mapView) {
      if(mRemoved) return;
      if (!hasNativeImages()) return;

      // add all images from drawables folder
      MapboxMap map = mapView.getMapboxMap();
      for (Map.Entry<String, BitmapDrawable> nativeImage : mNativeImages) {
        if (!hasNamedImage(nativeImage.getKey(), map)) {
          map.addImage(nativeImage.getKey(), nativeImage.getValue().getBitmap());
        }
      }
    }

    private void addRemoteImages(RCTMGLMapView mapView, @Nullable DownloadMapImageTask.OnAllImagesLoaded callback) {
      if(mRemoved) return;
      if (!hasImages() && callback != null) {
        callback.onAllImagesLoaded();
        return;
      }

      MapboxMap map = mapView.getMapboxMap();

      // find which images that are not yet added to the map style
      ArrayList<Map.Entry<String, String>> missingImages = new ArrayList<>();
      for (Map.Entry<String, String> image : mImages) {
        if (!hasNamedImage(image.getKey(), map)) {
          missingImages.add(image);
        }
      }

      if (missingImages.size() > 0) {
        // fetch images and add to style
        DownloadMapImageTask task = new DownloadMapImageTask(getContext(), map, callback);
        Map.Entry<String, String>[] params = missingImages.toArray(new Map.Entry[missingImages.size()]);
        task.execute(params);
      } else if (callback != null) {
        // no missing images, all images are loaded
        callback.onAllImagesLoaded();
      }
    }

    private boolean hasNamedImage(String name, MapboxMap map) {
     return map.getImage(name) != null;
    }


    private boolean hasImages() {
        return mImages != null && mImages.size() > 0;
    }

    private boolean hasNativeImages() {
        return mNativeImages != null && mNativeImages.size() > 0;
    }
}
