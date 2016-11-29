package ren.solid.ganhuoio.ui.fragment;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;
import ren.solid.ganhuoio.GanHuoIOApplication;
import ren.solid.ganhuoio.R;
import ren.solid.ganhuoio.constant.Apis;
import ren.solid.ganhuoio.model.bean.GanHuoDataBean;
import ren.solid.ganhuoio.model.bean.bomb.CollectTable;
import ren.solid.ganhuoio.utils.AppUtils;
import ren.solid.ganhuoio.utils.AuthorityUtils;
import ren.solid.ganhuoio.utils.DialogUtils;
import ren.solid.library.activity.ViewPicActivity;
import ren.solid.library.activity.WebViewActivity;
import ren.solid.library.adapter.SolidMultiItemTypeRVBaseAdapter;
import ren.solid.library.adapter.SolidRVBaseAdapter;
import ren.solid.library.fragment.XRecyclerViewFragment;
import ren.solid.library.http.HttpClientManager;
import ren.solid.library.utils.DateUtils;
import ren.solid.library.utils.Logger;
import ren.solid.library.utils.StringStyleUtils;
import ren.solid.library.utils.json.JsonConvert;
import ren.solid.library.widget.LinearDecoration;

/**
 * Created by _SOLID
 * Date:2016/4/19
 * Time:10:57
 */
public class CategoryListFragment extends XRecyclerViewFragment<GanHuoDataBean> {

    private String mType;

    @Override
    protected void customConfig() {
        addItemDecoration(new LinearDecoration(getContext(), RecyclerView.VERTICAL));
    }

    @Override
    protected List<GanHuoDataBean> parseData(String result) {

        List<GanHuoDataBean> list;
        JsonConvert<List<GanHuoDataBean>> jsonConvert = new JsonConvert<List<GanHuoDataBean>>() {
        };
        jsonConvert.setDataName("results");
        list = jsonConvert.parseData(result);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;

    }

    @Override
    protected String getUrl(int mCurrentPageIndex) {
        mType = getArguments().getString("type");
        String url = Apis.Urls.GanHuoData + mType + "/10/" + mCurrentPageIndex;
        return url;
    }

    @Override
    protected SolidRVBaseAdapter setAdapter() {

        return new SolidMultiItemTypeRVBaseAdapter<GanHuoDataBean>(getMContext(), new ArrayList<GanHuoDataBean>()) {

            private final static int VIEW_TYPE_NORMAL = 0;
            private final static int VIEW_TYPE_IMAGE = 1;

            @Override
            protected void onBindDataToView(SolidCommonViewHolder holder, final GanHuoDataBean bean, int position) {
                String date = bean.getPublishedAt().replace('T', ' ').replace('Z', ' ');
                if (mType.equals("all")) {
                    holder.getView(R.id.tv_tag).setVisibility(View.VISIBLE);
                }

                if (getItemViewType(position) == VIEW_TYPE_IMAGE) {
                    holder.setText(R.id.tv_tag, bean.getType());
                    holder.setText(R.id.tv_people, "by " + bean.getWho());
                    holder.setText(R.id.tv_time, DateUtils.friendlyTime(date));
                    ImageView imageView = holder.getView(R.id.iv_img);
                    HttpClientManager.displayImage(imageView, bean.getUrl());
                } else {
                    holder.setText(R.id.tv_people, "by " + bean.getWho());
                    holder.setText(R.id.tv_time, DateUtils.friendlyTime(date));
                    holder.setText(R.id.tv_tag, bean.getType());
                    holder.setText(R.id.tv_desc, bean.getDesc());
                    holder.setImage(R.id.iv_source, AppUtils.getResourseIDByUrl(bean.getUrl()));
                    isCollected(holder, bean);
                    holder.getView(R.id.iv_action).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DialogUtils.showActionPopWindow(mContext, v, new CollectTable(bean));
                        }
                    });
                }
            }

            @Override
            public int getItemLayoutID(int viewType) {
                if (viewType == VIEW_TYPE_IMAGE)
                    return R.layout.item_ganhuo_image;
                else {
                    return R.layout.item_ganhuo_normal;
                }
            }

            @Override
            public int getItemViewType(int position) {
                if (isImage(mBeans.get(position).getUrl())) {
                    return VIEW_TYPE_IMAGE;
                } else {
                    return VIEW_TYPE_NORMAL;
                }
            }

            @Override
            protected void onItemClick(int position) {
                String url = mBeans.get(position - 1).getUrl();
                ArrayList<String> images = new ArrayList<String>();
                images.add(url);
                if (!isImage(url)) {
                    Intent intent = new Intent(getMContext(), WebViewActivity.class);
                    intent.putExtra(WebViewActivity.WEB_URL, url);
                    intent.putExtra(WebViewActivity.TITLE, mBeans.get(position - 1).getDesc());
                    getMContext().startActivity(intent);
                } else {
                    Intent intent = new Intent(getMContext(), ViewPicActivity.class);
                    intent.putStringArrayListExtra(ViewPicActivity.IMG_URLS, images);
                    getMContext().startActivity(intent);
                }
            }
        };
    }

    private void isCollected(final SolidRVBaseAdapter<GanHuoDataBean>.SolidCommonViewHolder holder, final GanHuoDataBean bean) {

        if (TextUtils.isEmpty(AuthorityUtils.getUserName())) return;

        BmobQuery<CollectTable> query = new BmobQuery<>();
        query.addWhereEqualTo("username", AuthorityUtils.getUserName());
        query.addWhereEqualTo("url", bean.getUrl());
        query.findObjects(GanHuoIOApplication.getInstance(), new FindListener<CollectTable>() {
            @Override
            public void onSuccess(List<CollectTable> list) {
                if (list.size() > 0) {
                    final SpannableStringBuilder builder = new SpannableStringBuilder(bean.getDesc());
                    builder.append(StringStyleUtils.format(getMContext(), "(已收藏)", R.style.CollectedAppearance));
                    CharSequence descText = builder.subSequence(0, builder.length());
                    holder.setText(R.id.tv_desc, descText);
                }
            }

            @Override
            public void onError(int i, String s) {
                Logger.e("onError:" + s + " code:" + i);
            }
        });
    }

    public boolean isImage(String url) {
        return url.endsWith(".jpg") || url.endsWith(".png");
    }

    @Override
    protected RecyclerView.LayoutManager setLayoutManager() {
        return new LinearLayoutManager(getMContext());
    }


}