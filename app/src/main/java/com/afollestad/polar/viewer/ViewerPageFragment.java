package com.afollestad.polar.viewer;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.assent.AssentFragment;
import com.afollestad.polar.R;
import com.afollestad.polar.util.KeepRatio;
import com.afollestad.polar.util.WallpaperUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

/** @author Aidan Follestad (afollestad) */
public class ViewerPageFragment extends AssentFragment {

  private int thumbHeight;
  private int thumbWidth;

  @BindView(R.id.progress)
  ProgressBar mProgress;

  @BindView(R.id.photo)
  ImageView mPhoto;

  @BindView(R.id.thumbnail)
  ImageView thumbnail;

  private WallpaperUtils.Wallpaper mWallpaper;
  private boolean isActive;
  private int mIndex;
  private Unbinder unbinder;

  private boolean isFullImageLoaded;

  public String getTitle() {
    return mWallpaper.name;
  }

  public String getSubTitle() {
    return mWallpaper.author;
  }

  public static ViewerPageFragment create(
      WallpaperUtils.Wallpaper wallpaper, int index, int thumbWidth, int thumbHeight) {
    ViewerPageFragment frag = new ViewerPageFragment();
    frag.mWallpaper = wallpaper;
    Bundle args = new Bundle();
    args.putSerializable("wallpaper", wallpaper);
    args.putInt("index", index);
    args.putInt("thumbWidth", thumbWidth);
    args.putInt("thumbHeight", thumbHeight);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mWallpaper = (WallpaperUtils.Wallpaper) getArguments().getSerializable("wallpaper");
    mIndex = getArguments().getInt("index");
    thumbWidth = getArguments().getInt("thumbWidth");
    thumbHeight = getArguments().getInt("thumbHeight");
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    WallpaperUtils.download(getActivity(), mWallpaper, item.getItemId() == R.id.apply);
    return super.onOptionsItemSelected(item);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_viewer, container, false);
  }

  public ViewerPageFragment setIsActive(boolean active) {
    isActive = active;
    final ViewerActivity act = (ViewerActivity) getActivity();
    if (act != null && isActive) {
      act.mToolbar.setTitle(getTitle());
      act.mToolbar.setSubtitle(getSubTitle());
    }
    return this;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);
    ViewCompat.setTransitionName(thumbnail, "view_" + mIndex);
    mProgress.setVisibility(View.VISIBLE);
    mPhoto.setVisibility(View.INVISIBLE);

    Glide.with(this)
        .load(mWallpaper.getListingImageUrl())
        .apply(
            new RequestOptions()
                .priority(Priority.IMMEDIATE)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transform(new KeepRatio())
                .override(thumbWidth, thumbHeight))
        .listener(
            new RequestListener<Drawable>() {
              @Override
              public boolean onLoadFailed(
                  @Nullable GlideException e,
                  Object model,
                  Target<Drawable> target,
                  boolean isFirstResource) {
                return false;
              }

              @Override
              public boolean onResourceReady(
                  Drawable resource,
                  Object model,
                  Target<Drawable> target,
                  DataSource dataSource,
                  boolean isFirstResource) {
                if (isActive) {
                  ((ViewerActivity) getActivity()).supportStartPostponedEnterTransition();
                }
                mPhoto.postDelayed(
                    () -> loadFullPhoto(), ViewerActivity.SHARED_ELEMENT_TRANSITION_DURATION);
                return false;
              }
            })
        .into(thumbnail);
  }

  private void loadFullPhoto() {
    Glide.with(ViewerPageFragment.this)
        .load(mWallpaper.url)
        .apply(
            new RequestOptions()
                .transform(new KeepRatio())
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE))
        .listener(
            new RequestListener<Drawable>() {
              @Override
              public boolean onLoadFailed(
                  @Nullable GlideException e,
                  Object model,
                  Target<Drawable> target,
                  boolean isFirstResource) {
                mProgress.setVisibility(View.GONE);
                return false;
              }

              @Override
              public boolean onResourceReady(
                  Drawable resource,
                  Object model,
                  Target<Drawable> target,
                  DataSource dataSource,
                  boolean isFirstResource) {
                mProgress.setVisibility(View.GONE);
                mPhoto.setVisibility(View.VISIBLE);
                thumbnail.postDelayed(
                    () -> {
                      if (thumbnail != null) {
                        thumbnail.setVisibility(View.INVISIBLE);
                      }
                    },
                    500);

                ViewCompat.setTransitionName(mPhoto, "view_" + mIndex);
                ViewCompat.setTransitionName(thumbnail, null);
                isFullImageLoaded = true;
                return false;
              }
            })
        .into(mPhoto);
  }

  public ImageView getSharedElement() {
    return isFullImageLoaded ? mPhoto : thumbnail;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  @Override
  public void onResume() {
    super.onResume();
    setIsActive(isActive);
  }
}
