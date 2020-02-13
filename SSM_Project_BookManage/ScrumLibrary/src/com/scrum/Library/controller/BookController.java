package com.scrum.Library.controller;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.scrum.Library.parameter.Parameter;
import com.scrum.Library.service.BookClassService;
import com.scrum.Library.service.BookService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scrum.Library.domain.Book;
import com.scrum.Library.domain.BookClass;
import com.scrum.Library.util.DateExchangeUtil;
import com.scrum.Library.util.ExportExcelUtil;

//图书控制器
@Controller
@RequestMapping("/book")
@RequiresRoles(value={"BookManager","SysManager","Manager"},logical=Logical.OR)
public class BookController {
	@Autowired
	private BookService bookService;//图书服务
	@Autowired
	private BookClassService bookClassService;//图书编目服务


	//	进入图书信息录入的界面
	@RequiresPermissions(value={"book:toBook","iterm:all"},logical=Logical.OR)
	@RequestMapping("/toBook")
	public String intoBook(Model model)
	{
		List<BookClass> bookClassList=bookClassService.selectAllBkCatalog();
		model.addAttribute("classList",bookClassList);
		return "WEB-INF/bookJsp/book";
	}


//	保存图书信息
	@SuppressWarnings("unused")
	@RequestMapping(value="/insertBook",method={RequestMethod.POST,RequestMethod.GET})
	@RequiresPermissions(value={"book:insertBook","iterm:all"},logical=Logical.OR)
	public String InsertBook(@Validated Book book,BindingResult br,@RequestParam("DatePress")String datePress,
			@RequestParam("DateIn")String dateIn,@RequestParam(value="file",required=false)MultipartFile file,
			HttpServletRequest request,HttpServletResponse response,Model model) throws ParseException, IllegalStateException, IOException
	{
		book.setBkDatePress(DateExchangeUtil.StringToDate(datePress));
		book.setBkDateIn(DateExchangeUtil.StringToDate(dateIn));
		if(br.hasErrors())
		{
			List<ObjectError> errors=br.getAllErrors();
			for(ObjectError error:errors)
			{
				model.addAttribute("message",error.getDefaultMessage());
			}
			return "error";
		}
		
		String path=request.getSession().getServletContext().getRealPath("upload")+"/book";
		String fileName=file.getOriginalFilename();
		String fileType=file.getContentType();
		File targetFile = new File(path, fileName);   
		if(file==null)
		{
			book.setBkCover("upload/book/empty.jpg");
		}
		else
		{
			if(fileType.equals("image/jpeg")  ||fileType.equals("image/pjpeg")||fileType.equals("image/gif"))
			{
				book.setBkCover("upload/book/"+fileName);
				 file.transferTo(targetFile);  
			}
			else
			{
				book.setBkCover("upload/book/empty.jpg");
			}
			
		}
		bookService.insertBook(book);
		return "redirect:/book/toBook";
	}


	//	图书信息展示
	@RequestMapping(value="/findBookInfo",method={RequestMethod.POST,RequestMethod.GET})
	@RequiresPermissions(value={"book:findBookInfo","iterm:all"},logical=Logical.OR)
	public String FindBookInfo(Parameter parameter, Model model)
	{
		int count=bookService.getCounts(parameter);
		int start=parameter.getStart();
		if(start<0)start=0;
		if(start>count)start-=10;
		int end=start+10;
		parameter.setStart(start);
		parameter.setEnd(end);
		List<Book>bookList=this.bookService.selectBook(parameter);
		model.addAttribute("BookList",bookList);
		model.addAttribute("parameter", parameter);
		return "WEB-INF/bookJsp/bookInfo";
	}



//	删除图书
	@RequestMapping("/deleteBook")
	@RequiresPermissions(value={"book:deleteBook","iterm:all"},logical=Logical.OR)
	public String DeleteBook(Parameter parameter ,RedirectAttributes ra)
	{
		
		bookService.deleteBookByID(parameter.getBkID());
		ra.addAttribute("start", parameter.getStart());
		return "redirect:/book/findBookInfo";
	}


//	查找图书
	@RequiresPermissions(value={"book:searchBook","iterm:all"},logical=Logical.OR)
	@RequestMapping(value="/searchBook",method={RequestMethod.POST,RequestMethod.GET})
	public String SearchBook(Parameter parameter,RedirectAttributes ra)
	{
		System.out.println(parameter.getBkName());
		ra.addAttribute("start", parameter.getStart());
		ra.addAttribute("bkName", parameter.getBkName());
		return "redirect:/book/findBookInfo";
	}



//	跳转到图书信息编辑界面
	@RequiresPermissions(value={"book:editBook","iterm:all"},logical=Logical.OR)
	@RequestMapping("/editBook")
	public String EditBook(Parameter parameter,Model model)
	{
		List<Book> bookList=bookService.selectBook(parameter);
		model.addAttribute("book", bookList.get(0));
		List<BookClass> bookClassList=bookClassService.selectAllBkCatalog();
		model.addAttribute("classList",bookClassList);
		return "WEB-INF/bookJsp/bookEdit";
	}
	@SuppressWarnings("unused")
	@RequiresPermissions(value={"book:updateBook","iterm:all"},logical=Logical.OR)
	@RequestMapping(value="/updateBook",method={RequestMethod.POST,RequestMethod.GET})
	public String updateBook(@Validated Book book,BindingResult br,@RequestParam("DatePress")String datePress,
			@RequestParam("DateIn")String dateIn,@RequestParam(value="file",required=false)MultipartFile file,
			HttpServletRequest request,HttpServletResponse response,Model model) throws ParseException, IllegalStateException, IOException
	{
		book.setBkDatePress(DateExchangeUtil.StringToDate(datePress));
		book.setBkDateIn(DateExchangeUtil.StringToDate(dateIn));
		if(br.hasErrors())
		{
			List<ObjectError> errors=br.getAllErrors();
			for(ObjectError error:errors)
			{
				model.addAttribute("message",error.getDefaultMessage());
			}
			return "error";
		}
		
		String path=request.getSession().getServletContext().getRealPath("upload")+"/book";
		String fileName=file.getOriginalFilename();
		String fileType=file.getContentType();
		File targetFile = new File(path, fileName);   
		if(file==null)
		{
			book.setBkCover("upload/book/empty.jpg");
		}
		else
		{
			if(fileType.equals("image/jpeg")  ||fileType.equals("image/pjpeg")||fileType.equals("image/gif"))
			{
				book.setBkCover("upload/book/"+fileName);
				 file.transferTo(targetFile);  
			}
			else
			{
				book.setBkCover("upload/book/empty.jpg");
			}
			
		}
		bookService.updateBookByID(book);
		return "index";
	}




//	导出数据
	@RequestMapping("/exportBook")
	public String ExportExcel(Parameter parameter,HttpServletRequest request,HttpServletResponse response)
	{
		System.out.println(parameter.getStart());
		parameter.setEnd(parameter.getStart()+10);
		List<Book> bookList=bookService.selectBook(parameter);
		
		ExportExcelUtil ex = new ExportExcelUtil();
		String title="图书信息表";
		String[] headers ={"图书序号","图书编号","书名","作者","出版社","出版日期","ISBN书号","分类号","语言","页数","价格","入馆日期","内容简介","图书状态","图书封面"};
		List<String[]> dataset=new ArrayList<String[]>();
		for(int i=0;i<bookList.size();i++)
		{
			Book book=bookList.get(i);
			dataset.add(new String[]{book.getBkID(),book.getBkCode(),book.getBkName(),book.getBkAuthor(),
					book.getBkPress(),DateExchangeUtil.DateToString(book.getBkDatePress()),
					book.getBkISBN(),book.getBkCatalog(),book.getBkLanguage(),Integer.toString(book.getBkPages()),
					book.getBkPrice(),DateExchangeUtil.DateToString(book.getBkDateIn()),book.getBkBrief(),book.getBkStatus(),book.getBkCover()});
			
		}
		OutputStream out = null;//创建一个输出流对象 
		try { 
			
			out = response.getOutputStream();//
			response.setHeader("Content-disposition","attachment; filename="+"book.xls");//filename是下载的xls的名，建议最好用英文 
			response.setContentType("application/msexcel;charset=UTF-8");//设置类型 
			response.setHeader("Pragma","No-cache");//设置头 
			response.setHeader("Cache-Control","no-cache");//设置头 
			response.setDateHeader("Expires", 0);//设置日期头  
			String rootPath = request.getSession().getServletContext().getRealPath("/");
			ex.exportExcel(rootPath,title,headers, dataset, out);
			out.flush();
		} catch (IOException e) { 
			e.printStackTrace(); 
		}finally{
			try{
				if(out!=null){ 
					out.close(); 
				}
			}catch(IOException e){ 
				e.printStackTrace(); 
			} 
		}
		return "";
	}
}
