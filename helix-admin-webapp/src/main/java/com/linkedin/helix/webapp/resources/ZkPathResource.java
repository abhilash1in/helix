/**
 * Copyright (C) 2012 LinkedIn Inc <opensource@linkedin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.helix.webapp.resources;

import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.linkedin.helix.ZNRecord;
import com.linkedin.helix.manager.zk.ZkClient;
import com.linkedin.helix.webapp.RestAdminApplication;

public class ZkPathResource extends Resource
{
  private final static Logger LOG = Logger.getLogger(ZkPathResource.class);

  public ZkPathResource(Context context, Request request, Response response)
  {
    super(context, request, response);
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    getVariants().add(new Variant(MediaType.APPLICATION_JSON));
  }

  public boolean allowGet()
  {
    return true;
  }

  public boolean allowPost()
  {
    return true;
  }

  public boolean allowPut()
  {
    return false;
  }

  public boolean allowDelete()
  {
    return true;
  }
  
  public Representation represent(Variant variant)
  {
    StringRepresentation presentation = null;
    try
    {
      String zkPath = getZKPath();
      
      ZkClient zkClient = (ZkClient)getContext().getAttributes().get(RestAdminApplication.ZKCLIENT);;
      ZNRecord result = new ZNRecord("nodeContent");
      List<String> children = zkClient.getChildren(zkPath);
      if(children.size() > 0)
      {
        result.setListField("children", children);
      }
      else
      {
        result = zkClient.readData(zkPath);
      }
      presentation = new StringRepresentation(ClusterRepresentationUtil.ZNRecordToJson(result), MediaType.APPLICATION_JSON);
    }
    catch (Exception e)
    {
      String error = ClusterRepresentationUtil.getErrorAsJsonStringFromException(e);
      presentation = new StringRepresentation(error, MediaType.APPLICATION_JSON);

      LOG.error("", e);
    }
    return presentation;
  }
  
  String getZKPath()
  {
    String zkPath = "/" + getRequest().getResourceRef().getRelativeRef().toString();
    if(zkPath.equals("/.") || zkPath.endsWith("/"))
    {
      zkPath = zkPath.substring(0, zkPath.length() - 1);
    }
    if(zkPath.length() == 0)
    {
      zkPath = "/";
    }
    return zkPath;
  }

  @Override
  public void removeRepresentations()
  {
    try
    {
      String zkPath = getZKPath();
      
      ZkClient zkClient = (ZkClient)getContext().getAttributes().get(RestAdminApplication.ZKCLIENT);;
      zkClient.deleteRecursive(zkPath);
      getResponse().setStatus(Status.SUCCESS_OK);
    }
    catch(Exception e)
    {
      getResponse().setEntity(ClusterRepresentationUtil.getErrorAsJsonStringFromException(e),
          MediaType.APPLICATION_JSON);
      getResponse().setStatus(Status.SUCCESS_OK);
      LOG.error("", e);
    }
  }

}
