/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.android.web.service;

/**
 *
 * @author arsi
 */
public interface UsageService {
    
    public void increment(String id);
    
    public int getMaxCount();
    
    public int getCurrentCount();
    
}
