/**
 * This file is part of Todo.txt for Android, an app for managing your todo.txt file (http://todotxt.com).
 * <p>
 * Copyright (c) 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 * <p>
 * LICENSE:
 * <p>
 * Todo.txt for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p>
 * Todo.txt for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Todo.txt for Android. If not, see
 * <http://www.gnu.org/licenses/>.
 * <p>
 * Todo.txt for Android's source code is available at https://github.com/ginatrapani/todo.txt-android
 *
 * @author Todo.txt for Android contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 */

package com.todotxt.todotxttouch.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.todotxt.todotxttouch.Constants;
import com.todotxt.todotxttouch.R;
import com.todotxt.todotxttouch.TodoApplication;
import com.todotxt.todotxttouch.task.FilterFactory;
import com.todotxt.todotxttouch.task.Priority;
import com.todotxt.todotxttouch.task.Task;
import com.todotxt.todotxttouch.task.TaskBag;
import com.todotxt.todotxttouch.util.Strings;
import com.todotxt.todotxttouch.util.Util;

import java.util.ArrayList;
import java.util.List;

import static com.todotxt.todotxttouch.task.Priority.A;
import static com.todotxt.todotxttouch.task.Priority.B;
import static com.todotxt.todotxttouch.task.Priority.C;
import static com.todotxt.todotxttouch.task.Priority.D;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = ListRemoteViewsFactory.class.getName();

    TodoApplication m_app;
    TaskBag taskBag;
    List<Task> tasks = new ArrayList<Task>();

    public ListRemoteViewsFactory(Context applicationContext, Intent intent) {
        Log.d(TAG, "ListRemoteViewsFactory instantiated");

        m_app = (TodoApplication) applicationContext;
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        taskBag = m_app.getTaskBag();
        tasks = taskBag.getTasks(FilterFactory.generateAndFilter(m_app.m_prios,
                m_app.m_contexts, m_app.m_projects, m_app.m_search, false),
                m_app.sort.getComparator());
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public long getItemId(int position) {
        return tasks.get(position).getId();
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Task task = tasks.get(position);
        int priorityViewId = R.id.listwidget_taskprio_other;
        RemoteViews rv = new RemoteViews(m_app.getPackageName(), R.layout.listwidget_item);

        SpannableString ss = new SpannableString(task.inScreenFormat());
        Util.setGray(ss, task.getProjects());
        Util.setGray(ss, task.getContexts());

        if (task.isCompleted()) {
            ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        rv.setTextViewText(R.id.listwidget_tasktext, ss);
        final Priority priority = task.getPriority();
        rv.setTextViewText(priorityViewId, priority.inListFormat());

        // can't seem to resolve theme attributes inside the service, so resorting
        // to including all views in the layout and controlling visibility
        rv.setViewVisibility(R.id.listwidget_taskprio_a, priority == A ? View.VISIBLE : View.INVISIBLE);
        rv.setViewVisibility(R.id.listwidget_taskprio_b, priority == B ? View.VISIBLE : View.INVISIBLE);
        rv.setViewVisibility(R.id.listwidget_taskprio_c, priority == C ? View.VISIBLE : View.INVISIBLE);
        rv.setViewVisibility(R.id.listwidget_taskprio_d, priority == D ? View.VISIBLE : View.INVISIBLE);
        rv.setViewVisibility(R.id.listwidget_taskprio_other,
                (priority == A || priority == B || priority == C || priority == D)
                        ? View.INVISIBLE : View.VISIBLE);

        // bit clumsy but avoids defining the task priority text strings in xml and in the Priority class
        rv.setTextViewText(R.id.listwidget_taskprio_a, task.getPriority().inListFormat());
        rv.setTextViewText(R.id.listwidget_taskprio_b, task.getPriority().inListFormat());
        rv.setTextViewText(R.id.listwidget_taskprio_c, task.getPriority().inListFormat());
        rv.setTextViewText(R.id.listwidget_taskprio_d, task.getPriority().inListFormat());
        rv.setTextViewText(R.id.listwidget_taskprio_other, task.getPriority().inListFormat());

        if (m_app.m_prefs.isPrependDateEnabled()
                && !task.isCompleted()
                && !Strings.isEmptyOrNull(task.getRelativeAge())) {
            rv.setTextViewText(R.id.listwidget_taskage, task.getRelativeAge());
            rv.setViewVisibility(R.id.listwidget_taskage, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.listwidget_taskage, View.GONE);
        }

        // Set the click intent so that we can handle it
        final Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.EXTRA_TASK, getItemId(position));
        rv.setOnClickFillInIntent(R.id.listwidget_item, fillInIntent);

        return rv;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
    }
}
