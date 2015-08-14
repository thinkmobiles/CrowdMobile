package com.crowdmobile.kesapp.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.widget.FixedARImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gadza on 2015.03.11..
 */
public class GalleryFragment extends Fragment {

    public interface GalleryFragmentListener {
        public void onPictureSelected(int location[], Drawable d, String path);
    }

    class ImageData {
        String thumbnail;
        String image;
    }

    GalleryLoader loader;
    GalleryAdapter galleryAdapter;
    ArrayList<ImageData> list;
    GridView gridView;
    GalleryFragmentListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (GalleryFragmentListener)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list = new ArrayList<ImageData>();
        list.add(null);
        galleryAdapter = new GalleryAdapter(getActivity(), 0, list);
        loader = new GalleryLoader();
        loader.execute();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loader != null) {
            loader.cancel(true);
            loader = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_gallery,container,false);
        gridView = (GridView)result.findViewById(R.id.gvGallery);
        gridView.setAdapter(galleryAdapter);
        //gridView.setOnItemClickListener(new ItemClickListener());
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        gridView = null;
    }

    /*
    class ItemClickListener implements GridView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            listener.onPictureSelected(list.get(position));
        }
    };
    */

    View.OnClickListener imageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Drawable d = ((ImageView)v).getDrawable();
            int position = (Integer)v.getTag();
            int [] location = new int[3];
            location[0] = ((View)v.getParent()).getLeft();
            location[1] = ((View)v.getParent()).getTop();
            location[2] = v.getWidth();
            ImageData data = list.get(position);
            listener.onPictureSelected(location, d, data != null ? data.image : null);
            //v.setVisibility(View.INVISIBLE);
        }
    };

    class GalleryAdapter extends ArrayAdapter<ImageData> {

        private LayoutInflater inflater;
        private Context context;



        public GalleryAdapter(Context context, int resid, List<ImageData> objects) {
            super(context, resid, objects);
            this.context = context;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageData data = getItem(position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_gallery, parent, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.imgPhoto = (FixedARImageView) convertView.findViewById(R.id.ivPhoto);
                holder.imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.imgPhoto.setOnClickListener(imageClick);
            } else
                holder = (ViewHolder) convertView.getTag();

            holder.imgPhoto.setTag(Integer.valueOf(position));
            if (position != 0) {
                String fileName = data.thumbnail != null ? data.thumbnail : data.image;
                Picasso.with(context).load("file://" + fileName).fit().centerCrop().into(holder.imgPhoto,null);
            }
            else
                Picasso.with(context).load(R.drawable.ic_camera_gallery).fit().centerCrop().into(holder.imgPhoto);
            return convertView;
        }

        class ViewHolder {
            FixedARImageView imgPhoto;
        }
    }

    class GalleryLoader extends AsyncTask<Void ,Void, ArrayList<ImageData>> {
        @Override
        protected ArrayList<ImageData> doInBackground(Void... params) {
            SparseArray<ImageData> slist = new SparseArray<ImageData>();
            ArrayList<ImageData> retval = new ArrayList<ImageData>();
            String[] thumbnails = {MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
            String[] images = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};

            int col_id, col_data;

            Cursor cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, images, null, null, null);
            if (cursor != null) {
                col_id = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                col_data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                if (cursor.moveToFirst())
                    do {
                        ImageData data = new ImageData();
                        int id = cursor.getInt(col_id);
                        data.image = cursor.getString(col_data);
                        retval.add(data);
                        slist.put(id, data);
                    } while (cursor.moveToNext());
                cursor.close();
            }

            cursor = getActivity().getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbnails, null, null, null);
            if (cursor != null) {
                col_id = cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID);
                col_data = cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);

                if (cursor.moveToFirst())
                    do {
                        int id = cursor.getInt(col_id);
                        ImageData data = slist.get(id);
                        /*
                        if (data == null)
                        {
                            data = new ImageData();
                            data.thumbnail = cursor.getString(col_data);
                            retval.add(data);
                            slist.put(id,data);

                        }
                        */
                        if (data != null)
                            data.thumbnail = cursor.getString(col_data);
                    } while (cursor.moveToNext());
                cursor.close();
            }



            return retval;
        }

        @Override
        protected void onPostExecute(ArrayList<ImageData> retval) {
            if (retval != null) {
                for (int i = 0; i < retval.size(); i++)
                    list.add(retval.get(i));
                galleryAdapter.notifyDataSetChanged();
            }
            loader = null;
        }
    };


}
