package com.scrum.Library.controller;


import java.util.Date;
import java.util.List;

import com.scrum.Library.service.BorrowService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scrum.Library.domain.Borrow;
import com.scrum.Library.domain.ReaderType;
import com.scrum.Library.parameter.Parameter;
import com.scrum.Library.util.DateExchangeUtil;

//借书控制器
@Controller
@RequestMapping("/borrow")
@RequiresRoles(value={"BookManager","SysManager","Manager"},logical=Logical.OR)
public class BorrowContorller {
	@Autowired
	private BorrowService borrowService;

//	借书界面
	@RequestMapping("/toBorrow")
	@RequiresPermissions(value={"borrow:toBorrow","iterm:all"},logical=Logical.OR)
	public String comeBorrow() {
		return "WEB-INF/bookJsp/bookBorrow";
	}

//	插入借阅信息,读者信息表的借阅图书数加1，图书表的图书状态改变成借出
	@RequestMapping(value = "/insertBorrow", method = { RequestMethod.POST, RequestMethod.GET })
	@RequiresPermissions(value={"borrow:insertBorrow","iterm:all"},logical=Logical.OR)
	public String InsertBorrow(@Validated Borrow borrow, BindingResult br, Model model,
			@RequestParam("DateOut") String DateOut) throws Exception {

		if (br.hasErrors()) {
			String Message = null;
			List<ObjectError> errors = br.getAllErrors();
			for (ObjectError objectError : errors) {

				model.addAttribute("message", Message + objectError.getDefaultMessage());
			}
			return "error";
		}
		ReaderType readerType = borrowService.findReaderTypeByrdID(borrow.getRdID());
		borrow.setIdContinueTimes(0);
		borrow.setIdDateOut(DateExchangeUtil.StringToDate(DateOut));
		borrow.setIdDateRetPlan(
				DateExchangeUtil.GetDateFromDay(DateExchangeUtil.StringToDate(DateOut), readerType.getCanLendDay()));
		borrow.setIdDateRetAct(null);
		borrow.setIdOverDay(0);
		borrow.setIdOverMoney(0);
		borrow.setIdPunishMoney(0);
		borrow.setIsHasReturn("未还");
		borrow.setOperatorRet("");
		System.out.println(borrow.getBkID() + borrow.getRdID() + borrow.getOperatorLend());
		// 更新
		Parameter parameter = new Parameter();

		parameter.setBkID(borrow.getBkID());
		parameter.setRdID(borrow.getRdID());
		// 比较证件是否有效
		Parameter parameter1 = borrowService.getReaderInfoByID(parameter);
		if (parameter1.getRdStatus().equals("有效") && (DateExchangeUtil.GetDayFormDate(new Date(),
				parameter1.getRdDateReg()) <= readerType.getDateValid() * 366 || readerType.getDateValid() == 0)) {
			if (parameter1.getRdBorrowQty() <= readerType.getCanLendQty()) {
				if (parameter1.getBkStatus().equals("在馆")) {
					parameter.setBkStatus("借出");
					parameter.setRdStatus("有效");
					parameter.setRdBorrowQty(parameter1.getRdBorrowQty() + 1);
					borrowService.insertBorrow(borrow);
					borrowService.updateBookByID(parameter);
					borrowService.updateReaderByID(parameter);
				} else {
					throw new Exception("图书已借出");
				}

			} else {
				throw new Exception("已超过规定的借书的数量");
			}

		} else {

			parameter.setRdStatus("注销");
			borrowService.updateReaderByID(parameter1);
			throw new Exception("你的证件已过期");
		}

		return "index";
	}

//	列举所有的信息
	@RequestMapping("/selectBorrowInfo")
	@RequiresPermissions(value={"borrow:selectBorrowInfo","iterm:all"},logical=Logical.OR)
	public String SelectBorrowInfo(Parameter parameter,Model model)
	{
		int count=borrowService.getCounts(parameter);
		int start=parameter.getStart();
		if(start<0) start=0;
		if(start>count)start-=10;
		int end=start+10;
		parameter.setStart(start);
		parameter.setEnd(end);
		List<Borrow> borrowList=borrowService.selectBorrowInfo(parameter);
		model.addAttribute("BorrowList",borrowList);
		model.addAttribute("parameter", parameter);
		return "WEB-INF/bookJsp/bookBorrowInfo";
	}

//	删除借阅信息，把一切的信息还原
	@RequestMapping("deleteBorrow")
	@RequiresPermissions(value={"borrow:deleteBorrow","iterm:all"},logical=Logical.OR)
	public String DeleteBorrow(Parameter parameter,RedirectAttributes ra,@RequestParam("id")int id)
	{
		if(parameter.getIsHasReturn().equals("未还"))
		{
		Parameter pmt=borrowService.getReaderInfoByID(parameter);
		Parameter temp=new Parameter();
		temp.setBkID(parameter.getBkID());
		temp.setRdID(parameter.getRdID());
		temp.setRdBorrowQty(pmt.getRdBorrowQty()-1);
		temp.setBkStatus("在馆");
		borrowService.updateReader(temp);
		borrowService.updateBookByID(temp);
		}
		borrowService.deleteBorrowByID(id);
		ra.addAttribute("start",0);
		ra.addAttribute("isHasReturn", parameter.getIsHasReturn());
		return "redirect:/borrow/selectBorrowInfo";
	}


//	进入归还和续借的操作页面
	@RequestMapping("/comeBorrowEdit")
	@RequiresPermissions(value={"borrow:comeBorrowEdit","iterm:all"},logical=Logical.OR)
	public String comeBorrowEdit()
	{
		return "WEB-INF/bookJsp/bookBorrowEdit";
	}




	@RequestMapping(value = "/borrowOperator", method = { RequestMethod.POST, RequestMethod.GET })
	@RequiresPermissions(value={"borrow:borrowOperator","iterm:all"},logical=Logical.OR)
	public String BorrowOperator(@Validated Borrow borrow,BindingResult br,Model model,@RequestParam("tool")String radion,@RequestParam("operator")String operator
			) throws Exception
	{

		if (br.hasErrors()) {
			String Message = null;
			List<ObjectError> errors = br.getAllErrors();
			for (ObjectError objectError : errors) {

				model.addAttribute("message", Message + objectError.getDefaultMessage());
			}
			return "error";
		}
		Borrow row=borrowService.getBorrow(borrow);
		ReaderType readerType=borrowService.findReaderTypeByrdID(borrow.getRdID());
		if(row.getIsHasReturn().equals("已还"))
		{
			throw new Exception("该图书已还不需要此操作");
		}
		if(radion.equals("2"))
		{
			
			if(row.getIdContinueTimes()<readerType.getCanContinueTimes())
			{
				borrow.setIdDateOut(new Date());
				borrow.setIdContinueTimes(row.getIdContinueTimes()+1);
				borrow.setOperatorLend(operator);
				borrow.setIdDateRetPlan(DateExchangeUtil.GetDateFromDay(new Date(), readerType.getCanLendDay()));
				borrowService.updateTB(borrow);
			}
			else
			{
				throw new Exception("你已没有续借的机会");
			}
			
			
		}
		if(radion.equals("1"))
		{
			borrow.setIdDateRetAct(new Date());
			borrow.setIdOverDay((int)DateExchangeUtil.GetDayFormDate(new Date(), row.getIdDateRetPlan()));
			borrow.setIdOverMoney(borrow.getIdOverDay()*readerType.getPunishRate());
			borrow.setIdPunishMoney(borrow.getIdOverMoney());
			borrow.setIsHasReturn("已还");
			borrow.setOperatorRet(operator);
			borrowService.updateReturn(borrow);
			Parameter parameter=new Parameter();
			parameter.setBkID(borrow.getBkID());
			parameter.setRdID(borrow.getRdID());
			Parameter pmt=borrowService.getReaderInfoByID(parameter);
			Parameter temp=new Parameter();
			temp.setBkID(parameter.getBkID());
			temp.setRdID(parameter.getRdID());
			temp.setRdBorrowQty(pmt.getRdBorrowQty()-1);
			temp.setBkStatus("在馆");
			borrowService.updateReader(temp);
			borrowService.updateBookByID(temp);
		}
		
		return "index";
	}
}
