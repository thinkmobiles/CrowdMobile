package com.crowdmobile.kesapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by gadza on 2015.06.30..
 */
public class SuggestionDialog extends Dialog {

    private Context mContext;
    private ListView lvQuestions;
    private ArrayList<String> questions;
    private QuestionsAdapter adapter;

    public SuggestionDialog(Context context)
    {
        super(context);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContext = context;
        setContentView(R.layout.suggestiondlg);
        questions = new ArrayList<String>();
        lvQuestions = (ListView)findViewById(R.id.lvQuestions);
        adapter = new QuestionsAdapter(context,0);
        lvQuestions.setAdapter(adapter);
    }

    public void setItems(ArrayList<String>list)
    {
        questions = list;
        adapter.notifyDataSetChanged();
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener)
    {
        lvQuestions.setOnItemClickListener(listener);
    }

    class QuestionsAdapter extends ArrayAdapter<String> {

        class ViewHolder {
            TextView tvSuggestion;
        }

        private LayoutInflater inflater;
        private int background1,background2;

        public QuestionsAdapter(Context context, int resource) {
            super(context, resource);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            background1 = context.getResources().getColor(R.color.suggestion_background1);
            background2 = context.getResources().getColor(R.color.suggestion_background2);
        }

        @Override
        public int getCount() {
            return questions.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null)
            {
                convertView = inflater.inflate(R.layout.suggestionitem,parent,false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.tvSuggestion = (TextView)convertView.findViewById(R.id.tvSuggestion);
            } else
                holder = (ViewHolder)convertView.getTag();

            convertView.setBackgroundColor((position & 1) == 0 ? background1 : background2);
            holder.tvSuggestion.setText(questions.get(position));
            return convertView;
        }

    };

}
