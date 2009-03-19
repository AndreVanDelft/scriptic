/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.vm;

import java.util.*;
import static scriptic.tokens.ScripticParseTreeCodes.*;

public class FrameLookup {
	RootNode rootNode;
	  Map<FrameKey, CommPartnerFrame>    mapFrameKeyToCommPartnerFrame = new HashMap<FrameKey, CommPartnerFrame>();
	  Map<FrameKey, CommPartnerFrame>    mapFrameKeyToSendPartnerFrame = new HashMap<FrameKey, CommPartnerFrame>();
	  Map<FrameKey, CommPartnerFrame> mapFrameKeyToReceivePartnerFrame = new HashMap<FrameKey, CommPartnerFrame>();
	  Map<FrameKey,        CommFrame>           mapFrameKeyToCommFrame = new HashMap<FrameKey, CommFrame>();

	  public FrameLookup(RootNode rootNode) {this.rootNode = rootNode;}
	  
	  private CommPartnerFrame getCommPartnerFrame(Object owner, NodeTemplate template, ValueHolder[] indexValues, 
			  										Map<FrameKey, CommPartnerFrame> mapFrameKeyToPartnerFrame) {
		  FrameKey fk = new FrameKey(owner, template, indexValues);
		  CommPartnerFrame result;
		  if (!mapFrameKeyToPartnerFrame.containsKey(fk))
		  {
			  result = new CommPartnerFrame(owner, template, indexValues);
			  mapFrameKeyToPartnerFrame.put(fk, result);
			  result._sharedFrames = new CommFrame[template.relatedTemplates.length];
			  int i = 0;
			  for (NodeTemplate rt: template.relatedTemplates) {
				  result._sharedFrames[i++] = getCommFrame(owner, rt, indexValues, result);
			  }
		  }
		  else
		  {
			  result = mapFrameKeyToPartnerFrame.get(fk);
		  }
		  return result;
	  }

	  private SendPartnerFrame getSendPartnerFrame(Object owner, NodeTemplate template, ValueHolder[] indexValues, 
				Map<FrameKey, CommPartnerFrame> mapFrameKeyToPartnerFrame) {
		FrameKey fk = new FrameKey(owner, template, indexValues);
		SendPartnerFrame result;
		if (!mapFrameKeyToPartnerFrame.containsKey(fk))
		{
			result = new SendPartnerFrame(owner, template, indexValues);
		  mapFrameKeyToPartnerFrame.put(fk, result);
		  result._sharedFrames = new CommFrame[template.relatedTemplates.length];
		  int i = 0;
		  for (NodeTemplate rt: template.relatedTemplates) {
			  result._sharedFrames[i++] = getCommFrame(owner, rt, indexValues, result);
		  }
		}
		else
		{
			result = (SendPartnerFrame) mapFrameKeyToPartnerFrame.get(fk);
		}
		return result;
	  }
	  private ReceivePartnerFrame getReceivePartnerFrame(Object owner, NodeTemplate template, ValueHolder[] indexValues, 
				Map<FrameKey, CommPartnerFrame> mapFrameKeyToPartnerFrame) {
		FrameKey fk = new FrameKey(owner, template, indexValues);
		ReceivePartnerFrame result;
		if (!mapFrameKeyToPartnerFrame.containsKey(fk))
		{
			result = new ReceivePartnerFrame(owner, template, indexValues);
		  mapFrameKeyToPartnerFrame.put(fk, result);
		  result._sharedFrames = new CommFrame[template.relatedTemplates.length];
		  int i = 0;
		  for (NodeTemplate rt: template.relatedTemplates) {
			  result._sharedFrames[i++] = getCommFrame(owner, rt, indexValues, result);
		  }
		}
		else
		{
			result = (ReceivePartnerFrame) mapFrameKeyToPartnerFrame.get(fk);
		}
		return result;
	}


	  private CommFrame getCommFrame(Object owner, NodeTemplate template, ValueHolder[] indexValues, 
			  						CommPartnerFrame commPartnerFrame) {
		  FrameKey fk = new FrameKey(owner, template, indexValues);
		  CommFrame commFrame;
		  if (!mapFrameKeyToCommFrame.containsKey(fk))
		  {
			  switch (template.typeCode) {
		      case  CommunicationDeclarationCode: 
				  commFrame = new CommFrame(owner, template, indexValues, rootNode);
				  mapFrameKeyToCommFrame.put(fk, commFrame);
				  commFrame._partners = new CommPartnerFrame[template.relatedTemplates.length];
				  int i = 0;
				  for (NodeTemplate rt: template.relatedTemplates) {
					  commFrame._partners[i++] = getCommPartnerFrame(owner, rt, indexValues, mapFrameKeyToCommPartnerFrame);
				  }
				  break;
		      case  ChannelDeclarationCode: 
				  commFrame = new ChannelFrame(owner, template, indexValues, rootNode);
				  mapFrameKeyToCommFrame.put(fk, commFrame);
				  commFrame._partners = new ChannelPartnerFrame[2];
				  commFrame._partners[0] = getSendPartnerFrame(owner, template.relatedTemplates[0], indexValues, mapFrameKeyToSendPartnerFrame);
				  commFrame._partners[1] = getReceivePartnerFrame(owner, template.relatedTemplates[0], indexValues, mapFrameKeyToReceivePartnerFrame);
				  break;
		      case  SendChannelDeclarationCode: 
				  commFrame = new SendChannelFrame(owner, template, indexValues, rootNode);
				  mapFrameKeyToCommFrame.put(fk, commFrame);
				  commFrame._partners = new ChannelPartnerFrame[1];
				  commFrame._partners[0] = getSendPartnerFrame(owner, template.relatedTemplates[0], indexValues, mapFrameKeyToSendPartnerFrame);
				  break;
		      case  ReceiveChannelDeclarationCode: 
				  commFrame = new ReceiveChannelFrame(owner, template, indexValues, rootNode);
				  mapFrameKeyToCommFrame.put(fk, commFrame);
				  commFrame._partners = new ChannelPartnerFrame[1];
				  commFrame._partners[0] = getReceivePartnerFrame(owner, template.relatedTemplates[0], indexValues, mapFrameKeyToReceivePartnerFrame);
				  break;
			  default: return null;
			  }
		  }
		  else
		  {
			  commFrame = mapFrameKeyToCommFrame.get(fk);
			  // make sure the given commPartnerFrame is in the partner list...
			  
			  for (int i = 0; i<commFrame._partners.length; i++) {
				  // chop index for templates in case of ChannelFrames...
				  int templateIndex = Math.min(i, template.relatedTemplates.length-1);
				  NodeTemplate rt = template.relatedTemplates[templateIndex];
				  if (rt==commPartnerFrame.template)
				  {
					if ((commPartnerFrame instanceof ReceivePartnerFrame)
					&& template.typeCode==ChannelDeclarationCode
					&& i==0)
					{
						//at [0] must come the SendPartnerFrame, so continue
						continue;
					}
				    commFrame._partners[i] = commPartnerFrame;
				    break;
				  }
			  }
		  }
		  return commFrame;
	  }

	  void    addCommRequestNode(CommRequestNode n) {
		  getCommPartnerFrame(n.owner, n.template, null, mapFrameKeyToCommPartnerFrame).addRequest(n);
      }
	  void    addSendRequestNode(CommRequestNode n) {
		  getSendPartnerFrame(n.owner, n.template, n.indexValues(), mapFrameKeyToSendPartnerFrame)
		    .addRequest(n);
      }
	  void addReceiveRequestNode(CommRequestNode n) {
		  getReceivePartnerFrame(n.owner, n.template, n.indexValues(), mapFrameKeyToReceivePartnerFrame)
		     .addRequest(n);
      }

	public void removeCommRequestNode(CommRequestNode n) {
		CommPartnerFrame cpf = getCommPartnerFrame(n.owner, n.template, n.indexValues(), mapFrameKeyToCommPartnerFrame);
		if (0 == cpf.removeRequest(n))
		{
			FrameKey fk = new FrameKey(n.owner, n.template, n.indexValues());
			mapFrameKeyToCommPartnerFrame.remove(fk); 
			for (CommFrame commFrame: cpf._sharedFrames) {
				if (commFrame._activePartners==0) {
					FrameKey cfk = new FrameKey(commFrame.owner, commFrame.template, n.indexValues());
					mapFrameKeyToCommFrame.remove(cfk); 
				}
			}
		}
	}

	public void removeSendRequestNode(SendRequestNode n) {
		CommPartnerFrame cpf = getSendPartnerFrame(n.owner, n.template, n.indexValues(), mapFrameKeyToSendPartnerFrame);
		if (0 == cpf.removeRequest(n))
		{
			FrameKey fk = new FrameKey(n.owner, n.template, n.indexValues());
			mapFrameKeyToSendPartnerFrame.remove(fk); 
			for (CommFrame commFrame: cpf._sharedFrames) {
				if (commFrame._activePartners==0) {
					FrameKey cfk = new FrameKey(commFrame.owner, commFrame.template, n.indexValues());
					mapFrameKeyToCommFrame.remove(cfk); 
				}
			}
		}
	}

	public void removeReceiveRequestNode(ReceiveRequestNode n) {
		CommPartnerFrame cpf = getReceivePartnerFrame(n.owner, n.template, n.indexValues(), mapFrameKeyToReceivePartnerFrame);
		if (0 == cpf.removeRequest(n))
		{
			FrameKey fk = new FrameKey(n.owner, n.template, n.indexValues());
			mapFrameKeyToReceivePartnerFrame.remove(fk); 
			for (CommFrame commFrame: cpf._sharedFrames) {
				if (commFrame._activePartners==0) {
					FrameKey cfk = new FrameKey(commFrame.owner, commFrame.template, n.indexValues());
					mapFrameKeyToCommFrame.remove(cfk); 
				}
			}
		}
	}
}
